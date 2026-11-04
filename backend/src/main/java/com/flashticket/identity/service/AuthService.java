package com.flashticket.identity.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.JwtService;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.RoleCode;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.dto.ForgotPasswordRequest;
import com.flashticket.identity.dto.LoginRequest;
import com.flashticket.identity.dto.LoginResponse;
import com.flashticket.identity.dto.ResetPasswordRequest;
import com.flashticket.identity.dto.UserProfileResponse;
import com.flashticket.identity.entity.User;
import com.flashticket.identity.entity.UserProfile;
import com.flashticket.identity.enums.UserStatus;
import com.flashticket.identity.enums.VerifyCodePurpose;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 登录认证编排服务：登录、刷新、退出、找回/重置密码。
 *
 * <p>安全要点（见 doc/用户与账号模块设计.md 3.2/3.3/四/八）：
 * <ul>
 *   <li>未知账号与密码错误统一返回 AUTH_BAD_CREDENTIALS，不泄露账号存在性；</li>
 *   <li>登录失败按账号维度限流（5 次锁 10 分钟），成功后清除；</li>
 *   <li>后台登录（adminLogin）额外校验账号具备后台角色，普通 USER 被拒；</li>
 *   <li>Refresh Token 走 HttpOnly Cookie + Redis 轮换，本服务只产出令牌，Cookie 由控制器写入。</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UserDomainService userDomainService;
    private final RbacDomainService rbacDomainService;
    private final SessionService sessionService;
    private final LoginProtectionService loginProtection;
    private final VerificationCodeService verificationCodeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserDomainService userDomainService,
                       RbacDomainService rbacDomainService,
                       SessionService sessionService,
                       LoginProtectionService loginProtection,
                       VerificationCodeService verificationCodeService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userDomainService = userDomainService;
        this.rbacDomainService = rbacDomainService;
        this.sessionService = sessionService;
        this.loginProtection = loginProtection;
        this.verificationCodeService = verificationCodeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** 认证结果：响应体 + 需要写入 Cookie 的 Refresh Token。 */
    public record AuthResult(LoginResponse response, String refreshToken) {
    }

    /** 用户名/邮箱 + 密码登录（用户端）。 */
    public AuthResult login(LoginRequest request) {
        return doLogin(request, false);
    }

    /** 后台登录：账号必须具备后台角色（OPERATOR/FINANCE/ADMIN/SUPER_ADMIN）。 */
    public AuthResult adminLogin(LoginRequest request) {
        return doLogin(request, true);
    }

    private AuthResult doLogin(LoginRequest request, boolean requireBackendRole) {
        String account = normalizeAccount(request.account());
        loginProtection.assertNotLocked(account);

        User user = userDomainService.findUserByAccount(request.account());
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginProtection.recordFailure(account);
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        loginProtection.clear(account);

        // 后台登录：先校验后台角色再签发，避免普通用户拿到后台会话
        if (requireBackendRole && !hasBackendRole(user.getId())) {
            throw new BizException(ErrorCode.AUTH_ADMIN_FORBIDDEN);
        }

        userDomainService.updateLastLoginAt(user.getId(), java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        return issueTokens(user);
    }

    /** 刷新：轮换 Refresh Token，重新签发 Access Token。 */
    public AuthResult refresh(String refreshToken) {
        SessionService.RotatedSession rotated = sessionService.rotateRefreshToken(refreshToken);
        Long userId = sessionService.getUserId(rotated.sessionId());
        if (userId == null) {
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        User user = userDomainService.findUserById(userId);
        if (user == null || !UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        List<String> roles = userDomainService.findRoleCodes(userId);
        List<String> permissions = rbacDomainService.findPermissionCodes(userId);
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions, rotated.sessionId());
        UserProfileResponse profile = toProfile(user, roles, permissions);
        return new AuthResult(
                new LoginResponse(accessToken, jwtService.getAccessTokenTtl().toSeconds(), profile),
                rotated.refreshToken());
    }

    /** 退出当前会话。 */
    public void logout(String sessionId) {
        sessionService.revokeSession(sessionId);
    }

    /** 退出全部设备。 */
    public void logoutAll(Long userId) {
        sessionService.revokeAllSessions(userId);
    }

    /** 找回密码第一步：发送重置验证码（未知邮箱静默成功，不泄露存在性）。 */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        User user = userDomainService.findUserByAccount(email);
        if (user == null) {
            return;
        }
        verificationCodeService.sendCode(email, VerifyCodePurpose.RESET_PASSWORD);
    }

    /** 找回密码第二步：验证码核销 + 重置密码 + 撤销全部会话。 */
    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        verificationCodeService.verify(email, VerifyCodePurpose.RESET_PASSWORD, request.verifyCode());

        User user = userDomainService.findUserByAccount(email);
        if (user == null) {
            // 验证码只发给真实存在的账号，此处为兜底，不泄露存在性
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        PasswordPolicy.assertStrong(request.newPassword());
        userDomainService.updatePasswordHash(user.getId(), passwordEncoder.encode(request.newPassword()));
        sessionService.revokeAllSessions(user.getId());
    }

    /* ---------- 内部 ---------- */

    private AuthResult issueTokens(User user) {
        List<String> roles = userDomainService.findRoleCodes(user.getId());
        List<String> permissions = rbacDomainService.findPermissionCodes(user.getId());
        SessionService.SessionIssued issued = sessionService.createSession(user.getId(), "web");
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions, issued.sessionId());
        UserProfileResponse profile = toProfile(user, roles, permissions);
        return new AuthResult(
                new LoginResponse(accessToken, jwtService.getAccessTokenTtl().toSeconds(), profile),
                issued.refreshToken());
    }

    private boolean hasBackendRole(Long userId) {
        List<String> roles = userDomainService.findRoleCodes(userId);
        return roles.stream().anyMatch(RoleCode::isBackendRole);
    }

    private UserProfileResponse toProfile(User user, List<String> roles, List<String> permissions) {
        UserProfile profile = userDomainService.findProfile(user.getId());
        String nickname = profile == null ? user.getUsername() : profile.getNickname();
        String avatarUrl = profile == null ? null : profile.getAvatarUrl();
        return new UserProfileResponse(
                String.valueOf(user.getId()), user.getUsername(), nickname,
                user.getEmail(), avatarUrl, roles, permissions);
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
