package com.flashticket.identity.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.common.web.RefreshCookieHelper;
import com.flashticket.identity.dto.ForgotPasswordRequest;
import com.flashticket.identity.dto.LoginRequest;
import com.flashticket.identity.dto.LoginResponse;
import com.flashticket.identity.dto.RegisterRequest;
import com.flashticket.identity.dto.RegisterResponse;
import com.flashticket.identity.dto.ResetPasswordRequest;
import com.flashticket.identity.dto.SendVerifyCodeRequest;
import com.flashticket.identity.service.AuthService;
import com.flashticket.identity.service.RegistrationService;
import com.flashticket.identity.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 认证与账号 REST 入口（M01 注册 + M02 登录/刷新/退出/找回密码）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final VerificationCodeService verificationCodeService;
    private final RegistrationService registrationService;
    private final AuthService authService;

    private final String refreshCookieName;
    private final Duration refreshTtl;

    public AuthController(VerificationCodeService verificationCodeService,
                          RegistrationService registrationService,
                          AuthService authService,
                          @Value("${flashticket.jwt.refresh-cookie}") String refreshCookieName,
                          @Value("${flashticket.jwt.refresh-token-ttl}") Duration refreshTtl) {
        this.verificationCodeService = verificationCodeService;
        this.registrationService = registrationService;
        this.authService = authService;
        this.refreshCookieName = refreshCookieName;
        this.refreshTtl = refreshTtl;
    }

    /** 发送邮箱验证码（匿名可访问）。 */
    @PostMapping("/verify-code/send")
    public ApiResponse<Void> sendVerifyCode(@Valid @RequestBody SendVerifyCodeRequest request) {
        verificationCodeService.sendCode(request.getEmail(), request.getPurpose());
        return ApiResponse.ok();
    }

    /** 用户注册（匿名可访问）。 */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(registrationService.register(request));
    }

    /** 登录（匿名可访问）：签发 Access Token + 写入 Refresh Token Cookie。 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        RefreshCookieHelper.write(response, refreshCookieName, result.refreshToken(), refreshTtl);
        return ApiResponse.ok(result.response());
    }

    /** 刷新（匿名可访问）：轮换 Refresh Token + 重签 Access Token。 */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(HttpServletRequest request,
                                              HttpServletResponse response) {
        String refreshToken = RefreshCookieHelper.read(request, refreshCookieName);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(
                    com.flashticket.common.api.ErrorCode.AUTH_REFRESH_INVALID, "缺少登录凭据");
        }
        AuthService.AuthResult result = authService.refresh(refreshToken);
        RefreshCookieHelper.write(response, refreshCookieName, result.refreshToken(), refreshTtl);
        return ApiResponse.ok(result.response());
    }

    /** 退出当前设备（需认证）。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        String sessionId = SecurityUtils.currentSessionId();
        authService.logout(sessionId);
        RefreshCookieHelper.clear(response, refreshCookieName);
        return ApiResponse.ok();
    }

    /** 退出全部设备（需认证）。 */
    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(HttpServletResponse response) {
        Long userId = SecurityUtils.currentUserId();
        authService.logoutAll(userId);
        RefreshCookieHelper.clear(response, refreshCookieName);
        return ApiResponse.ok();
    }

    /** 找回密码第一步：发送重置验证码（匿名，未知邮箱静默成功）。 */
    @PostMapping("/password/forgot")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok();
    }

    /** 找回密码第二步：验证码重置（匿名）。 */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }
}
