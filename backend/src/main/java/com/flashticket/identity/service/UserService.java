package com.flashticket.identity.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.dto.ChangePasswordRequest;
import com.flashticket.identity.dto.UpdateProfileRequest;
import com.flashticket.identity.dto.UserProfileResponse;
import com.flashticket.identity.entity.User;
import com.flashticket.identity.entity.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 当前用户个人中心服务：资料查询/修改、密码修改。
 */
@Service
public class UserService {

    private final UserDomainService userDomainService;
    private final RbacDomainService rbacDomainService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDomainService userDomainService,
                       RbacDomainService rbacDomainService,
                       SessionService sessionService,
                       PasswordEncoder passwordEncoder) {
        this.userDomainService = userDomainService;
        this.rbacDomainService = rbacDomainService;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
    }

    /** 查询当前用户资料。 */
    public UserProfileResponse getProfile(Long userId) {
        User user = requireUser(userId);
        List<String> roles = userDomainService.findRoleCodes(userId);
        List<String> permissions = rbacDomainService.findPermissionCodes(userId);
        UserProfile profile = userDomainService.findProfile(userId);
        return new UserProfileResponse(
                String.valueOf(user.getId()), user.getUsername(),
                profile == null ? user.getUsername() : profile.getNickname(),
                user.getEmail(),
                profile == null ? null : profile.getAvatarUrl(),
                roles,
                permissions);
    }

    /** 修改当前用户资料（昵称/头像）。 */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        requireUser(userId);
        String nickname = request.nickname().trim();
        if (nickname.length() < 2 || nickname.length() > 32) {
            throw new BizException(ErrorCode.PARAM_ERROR, "昵称长度需为 2-32 字符");
        }
        String avatarUrl = request.avatarUrl() == null ? "" : request.avatarUrl().trim();
        userDomainService.updateProfile(userId, nickname, avatarUrl);
        return getProfile(userId);
    }

    /** 修改密码：验证旧密码，撤销其他会话（保留当前）。 */
    @Transactional
    public void changePassword(Long userId, String sessionId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.AUTH_OLD_PASSWORD_WRONG);
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BizException(ErrorCode.AUTH_PASSWORD_SAME);
        }
        PasswordPolicy.assertStrong(request.newPassword());
        userDomainService.updatePasswordHash(userId, passwordEncoder.encode(request.newPassword()));
        // 修改密码后撤销其他会话，当前会话保留（见 doc/用户与账号模块设计.md 九）
        sessionService.revokeOtherSessions(userId, sessionId);
    }

    private User requireUser(Long userId) {
        User user = userDomainService.findUserById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
