package com.flashticket.identity.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.common.web.RefreshCookieHelper;
import com.flashticket.identity.dto.LoginRequest;
import com.flashticket.identity.dto.LoginResponse;
import com.flashticket.identity.service.AuthService;
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
 * 后台认证入口（M03）：管理员登录/刷新/退出。
 *
 * <p>与用户端共享认证服务，但使用独立 Refresh Cookie（ft_admin_refresh_token），
 * 且登录额外校验账号具备后台角色（OPERATOR/FINANCE/ADMIN/SUPER_ADMIN），
 * 普通 USER 账号即使密码正确也被拒绝（见 doc/用户与账号模块设计.md 3.2/八）。
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AuthService authService;
    private final String adminRefreshCookie;
    private final Duration refreshTtl;

    public AdminAuthController(AuthService authService,
                               @Value("${flashticket.jwt.admin-refresh-cookie}") String adminRefreshCookie,
                               @Value("${flashticket.jwt.refresh-token-ttl}") Duration refreshTtl) {
        this.authService = authService;
        this.adminRefreshCookie = adminRefreshCookie;
        this.refreshTtl = refreshTtl;
    }

    /** 后台登录（匿名可访问）：校验后台角色后签发令牌。 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletResponse response) {
        AuthService.AuthResult result = authService.adminLogin(request);
        RefreshCookieHelper.write(response, adminRefreshCookie, result.refreshToken(), refreshTtl);
        return ApiResponse.ok(result.response());
    }

    /** 后台刷新（匿名可访问）：轮换 Refresh Token + 重签 Access Token。 */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(HttpServletRequest request,
                                              HttpServletResponse response) {
        String refreshToken = RefreshCookieHelper.read(request, adminRefreshCookie);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(ErrorCode.AUTH_REFRESH_INVALID, "缺少登录凭据");
        }
        AuthService.AuthResult result = authService.refresh(refreshToken);
        RefreshCookieHelper.write(response, adminRefreshCookie, result.refreshToken(), refreshTtl);
        return ApiResponse.ok(result.response());
    }

    /** 后台退出（需认证）。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        authService.logout(SecurityUtils.currentSessionId());
        RefreshCookieHelper.clear(response, adminRefreshCookie);
        return ApiResponse.ok();
    }
}
