package com.flashticket.identity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.RegisteredUser;
import com.flashticket.identity.domain.RoleCode;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.dto.AdminResetPasswordRequest;
import com.flashticket.identity.dto.AssignRolesRequest;
import com.flashticket.identity.dto.CreateStaffRequest;
import com.flashticket.identity.dto.UpdateStaffRequest;
import com.flashticket.identity.dto.UpdateUserStatusRequest;
import com.flashticket.identity.dto.UserListItemResponse;
import com.flashticket.identity.entity.Role;
import com.flashticket.identity.entity.User;
import com.flashticket.identity.enums.UserStatus;
import com.flashticket.identity.mapper.UserMapper;
import com.flashticket.identity.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台账号管理编排服务（M03 RBAC）：
 * 用户列表、创建/修改员工、启用禁用、角色分配、强制下线、重置密码。
 *
 * <p>安全规则（见 doc/用户与账号模块设计.md 八）：
 * <ul>
 *   <li>只有 SUPER_ADMIN 能授予 SUPER_ADMIN 角色（ADMIN 无权）；</li>
 *   <li>操作者不能提升自己的权限；</li>
 *   <li>系统至少保留一个有效超级管理员，不能禁用/剥夺最后一个；</li>
 *   <li>禁用、角色变更、重置密码立即撤销目标用户会话；</li>
 *   <li>所有敏感操作写入审计。</li>
 * </ul>
 */
@Service
public class AdminUserService {

    private final UserDomainService userDomainService;
    private final RbacDomainService rbacDomainService;
    private final SessionService sessionService;
    private final OperationAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminUserService(UserDomainService userDomainService,
                            RbacDomainService rbacDomainService,
                            SessionService sessionService,
                            OperationAuditService auditService,
                            PasswordEncoder passwordEncoder,
                            UserMapper userMapper,
                            UserRoleMapper userRoleMapper) {
        this.userDomainService = userDomainService;
        this.rbacDomainService = rbacDomainService;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /** 用户列表（分页 + 用户名模糊/状态/角色筛选）。 */
    public PageResult<UserListItemResponse> listUsers(long page, long pageSize,
                                                      String username, String status, String roleCode) {
        Page<User> p = new Page<>(page, pageSize);
        IPage<User> result = userMapper.selectUserPage(p,
                normalize(username), normalize(status), normalize(roleCode));

        List<User> users = result.getRecords();
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, String> nicknames = userDomainService.findNicknames(userIds);
        Map<Long, List<String>> roleMap = roleMapOf(userIds);

        List<UserListItemResponse> items = users.stream().map(u -> {
            String nickname = nicknames.getOrDefault(u.getId(), u.getUsername());
            List<String> roles = roleMap.getOrDefault(u.getId(), List.of());
            return new UserListItemResponse(
                    String.valueOf(u.getId()), u.getUsername(), u.getEmail(),
                    nickname, u.getStatus(), roles, u.getCreatedAt());
        }).toList();

        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 创建员工（管理员设置初始密码，不返回明文）。 */
    @Transactional
    public UserListItemResponse createStaff(CreateStaffRequest req, AuthUser operator, String ip) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String nickname = req.nickname().trim();

        PasswordPolicy.assertStrong(req.password());
        userDomainService.assertUsernameAvailable(username);
        userDomainService.assertEmailAvailable(email);
        assertAssignableRoles(operator, req.roleIds(), null);

        String hash = passwordEncoder.encode(req.password());
        RegisteredUser created = userDomainService.createUser(username, email, nickname, hash);
        rbacDomainService.assignUserRoles(created.userId(), req.roleIds());

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.USER_CREATE,
                OperationAction.TARGET_USER, created.userId(),
                Map.of("username", username, "email", email, "roleIds", req.roleIds()), ip);

        List<String> roles = rbacDomainService.findRolesByIds(req.roleIds()).stream()
                .map(Role::getCode).toList();
        return new UserListItemResponse(String.valueOf(created.userId()), username, email,
                nickname, UserStatus.ACTIVE.name(), roles, null);
    }

    /** 修改员工（昵称/邮箱）。 */
    @Transactional
    public UserListItemResponse updateStaff(Long userId, UpdateStaffRequest req, AuthUser operator, String ip) {
        User target = requireUser(userId);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equalsIgnoreCase(target.getEmail())) {
            userDomainService.assertEmailAvailable(email);
        }
        userDomainService.updateProfile(userId, req.nickname().trim(), null);
        userDomainService.updateEmail(userId, email);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.USER_UPDATE,
                OperationAction.TARGET_USER, userId,
                Map.of("nickname", req.nickname().trim(), "email", email), ip);

        return toItem(userId);
    }

    /** 启用/禁用账号，禁用立即撤销全部会话。 */
    @Transactional
    public void changeStatus(Long userId, UpdateUserStatusRequest req, AuthUser operator, String ip) {
        User target = requireUser(userId);
        String status = req.status();

        if (userId.equals(operator.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能对自己执行该操作");
        }
        if (UserStatus.DISABLED.name().equals(status)) {
            assertNotLastSuperAdmin(userId);
        }

        userDomainService.updateStatus(userId, status);
        if (UserStatus.DISABLED.name().equals(status)) {
            sessionService.revokeAllSessions(userId);
        }

        String action = UserStatus.DISABLED.name().equals(status)
                ? OperationAction.USER_DISABLE : OperationAction.USER_ENABLE;
        auditService.record(operator.getUserId(), operator.getUsername(), action,
                OperationAction.TARGET_USER, userId, Map.of("status", status), ip);
    }

    /** 分配角色（整体替换），变更后撤销目标会话强制重新登录。 */
    @Transactional
    public void assignRoles(Long userId, AssignRolesRequest req, AuthUser operator, String ip) {
        requireUser(userId);
        assertAssignableRoles(operator, req.roleIds(), userId);

        List<String> before = userDomainService.findRoleCodes(userId);
        rbacDomainService.assignUserRoles(userId, req.roleIds());
        List<String> after = rbacDomainService.findRolesByIds(req.roleIds()).stream()
                .map(Role::getCode).toList();

        // 后台权限变化使相关会话失效（见 doc/用户与账号模块设计.md 八）
        sessionService.revokeAllSessions(userId);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.ROLE_ASSIGN,
                OperationAction.TARGET_USER, userId,
                Map.of("before", before, "after", after), ip);
    }

    /** 强制下线：撤销指定用户全部会话。 */
    public void revokeSessions(Long userId, AuthUser operator, String ip) {
        requireUser(userId);
        sessionService.revokeAllSessions(userId);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SESSION_REVOKE,
                OperationAction.TARGET_USER, userId, null, ip);
    }

    /** 管理员重置员工密码（不返回明文），重置后撤销全部会话。 */
    @Transactional
    public void resetPassword(Long userId, AdminResetPasswordRequest req, AuthUser operator, String ip) {
        requireUser(userId);
        PasswordPolicy.assertStrong(req.newPassword());
        userDomainService.updatePasswordHash(userId, passwordEncoder.encode(req.newPassword()));
        sessionService.revokeAllSessions(userId);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.PASSWORD_RESET,
                OperationAction.TARGET_USER, userId, null, ip);
    }

    /* ---------- 内部 ---------- */

    private User requireUser(Long userId) {
        User user = userDomainService.findUserById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 角色分配越权校验：
     * 1) 角色必须全部有效；
     * 2) 授予 SUPER_ADMIN 仅限 SUPER_ADMIN 操作者；
     * 3) 操作者不能提升自己的权限；
     * 4) 不能剥夺最后一个有效超级管理员的 SUPER_ADMIN 角色。
     *
     * @param targetUserId 目标用户；创建员工时传 null（无「自我提升/最后超管」语义）。
     */
    private void assertAssignableRoles(AuthUser operator, List<Long> roleIds, Long targetUserId) {
        List<Role> roles = rbacDomainService.findRolesByIds(roleIds);
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw new BizException(ErrorCode.RBAC_ROLE_NOT_FOUND);
        }

        boolean containsSuperAdmin = roles.stream().anyMatch(r -> RoleCode.SUPER_ADMIN.equals(r.getCode()));
        boolean operatorIsSuperAdmin = operator.getRoles() != null
                && operator.getRoles().contains(RoleCode.SUPER_ADMIN);

        if (containsSuperAdmin && !operatorIsSuperAdmin) {
            throw new BizException(ErrorCode.RBAC_ROLE_NOT_GRANTABLE);
        }

        if (targetUserId != null && operator.getUserId().equals(targetUserId)) {
            int operatorMaxRank = (operator.getRoles() == null ? List.<String>of() : operator.getRoles())
                    .stream().mapToInt(RoleCode::rank).max().orElse(0);
            int newMaxRank = roles.stream().mapToInt(r -> RoleCode.rank(r.getCode())).max().orElse(0);
            if (newMaxRank > operatorMaxRank) {
                throw new BizException(ErrorCode.RBAC_SELF_ESCALATION);
            }
        }

        if (targetUserId != null
                && rbacDomainService.isActiveSuperAdmin(targetUserId)
                && rbacDomainService.countActiveSuperAdmins() <= 1
                && !containsSuperAdmin) {
            throw new BizException(ErrorCode.RBAC_LAST_SUPER_ADMIN);
        }
    }

    private void assertNotLastSuperAdmin(Long userId) {
        if (rbacDomainService.isActiveSuperAdmin(userId) && rbacDomainService.countActiveSuperAdmins() <= 1) {
            throw new BizException(ErrorCode.RBAC_LAST_SUPER_ADMIN);
        }
    }

    private UserListItemResponse toItem(Long userId) {
        User user = requireUser(userId);
        List<String> roles = userDomainService.findRoleCodes(userId);
        Map<Long, String> nicknames = userDomainService.findNicknames(List.of(userId));
        String nickname = nicknames.getOrDefault(userId, user.getUsername());
        return new UserListItemResponse(String.valueOf(user.getId()), user.getUsername(),
                user.getEmail(), nickname, user.getStatus(), roles, user.getCreatedAt());
    }

    private Map<Long, List<String>> roleMapOf(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = userRoleMapper.findRoleCodesByUserIds(userIds);
        Map<Long, List<String>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long uid = ((Number) row.get("userId")).longValue();
            String code = (String) row.get("code");
            map.computeIfAbsent(uid, k -> new ArrayList<>()).add(code);
        }
        return map;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
