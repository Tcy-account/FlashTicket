package com.flashticket.identity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.dto.AuditLogResponse;
import com.flashticket.identity.dto.PermissionResponse;
import com.flashticket.identity.dto.RoleResponse;
import com.flashticket.identity.entity.OperationAudit;
import com.flashticket.identity.entity.Permission;
import com.flashticket.identity.entity.Role;
import com.flashticket.identity.mapper.OperationAuditMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 角色与权限管理编排服务（M03 RBAC）：
 * 角色列表、权限点列表、角色权限查询、配置角色权限（SUPER_ADMIN）、审计日志查询。
 *
 * <p>配置角色权限属于全局授权，仅 SUPER_ADMIN 可执行（方法级 {@code @PreAuthorize} 兜底），
 * 且变更后立即撤销持有该角色用户的全部会话，强制重新登录以刷新权限。
 */
@Service
public class RolePermissionService {

    private final RbacDomainService rbacDomainService;
    private final SessionService sessionService;
    private final OperationAuditService auditService;
    private final OperationAuditMapper auditMapper;

    public RolePermissionService(RbacDomainService rbacDomainService,
                                 SessionService sessionService,
                                 OperationAuditService auditService,
                                 OperationAuditMapper auditMapper) {
        this.rbacDomainService = rbacDomainService;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.auditMapper = auditMapper;
    }

    /** 角色列表。 */
    public List<RoleResponse> listRoles() {
        return rbacDomainService.listRoles().stream().map(this::toRoleResponse).toList();
    }

    /** 权限点列表。 */
    public List<PermissionResponse> listPermissions() {
        return rbacDomainService.listPermissions().stream().map(this::toPermissionResponse).toList();
    }

    /** 某角色已绑定的权限点。 */
    public List<PermissionResponse> getRolePermissions(Long roleId) {
        requireRole(roleId);
        return rbacDomainService.findPermissionsByRoleId(roleId).stream()
                .map(this::toPermissionResponse).toList();
    }

    /** 配置角色权限点（整体替换）。 */
    @Transactional
    public void grantRolePermissions(Long roleId, List<Long> permissionIds, AuthUser operator, String ip) {
        Role role = requireRole(roleId);
        rbacDomainService.grantRolePermissions(roleId, permissionIds);

        // 权限变更使持有该角色的会话失效，强制重新登录（见 doc/用户与账号模块设计.md 八）
        List<Long> userIds = rbacDomainService.findUserIdsByRoleId(roleId);
        userIds.forEach(sessionService::revokeAllSessions);

        auditService.record(operator.getUserId(), operator.getUsername(),
                OperationAction.ROLE_PERMISSION_GRANT, OperationAction.TARGET_ROLE, roleId,
                Map.of("roleCode", role.getCode(), "permissionIds",
                        permissionIds == null ? List.of() : permissionIds), ip);
    }

    /** 审计日志分页查询（按时间倒序）。 */
    public PageResult<AuditLogResponse> listAuditLogs(long page, long pageSize) {
        Page<OperationAudit> p = new Page<>(page, pageSize);
        IPage<OperationAudit> result = auditMapper.selectPage(p,
                new LambdaQueryWrapper<OperationAudit>().orderByDesc(OperationAudit::getCreatedAt));
        List<AuditLogResponse> items = result.getRecords().stream()
                .map(this::toAuditResponse).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /* ---------- 内部 ---------- */

    private Role requireRole(Long roleId) {
        Role role = rbacDomainService.findRoleById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.RBAC_ROLE_NOT_FOUND);
        }
        return role;
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(String.valueOf(role.getId()), role.getCode(),
                role.getName(), role.getStatus());
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(String.valueOf(permission.getId()), permission.getCode(),
                permission.getName(), permission.getType());
    }

    private AuditLogResponse toAuditResponse(OperationAudit audit) {
        return new AuditLogResponse(
                String.valueOf(audit.getId()),
                String.valueOf(audit.getOperatorId()),
                audit.getOperatorUsername(),
                audit.getAction(),
                audit.getTargetType(),
                audit.getTargetId() == null ? null : String.valueOf(audit.getTargetId()),
                audit.getDetail(),
                audit.getIp(),
                audit.getCreatedAt());
    }
}
