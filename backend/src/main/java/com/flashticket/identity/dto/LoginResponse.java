package com.flashticket.identity.dto;

/**
 * 登录/刷新响应：Access Token + 用户资料摘要。
 *
 * <p>Refresh Token 不在此返回，走 HttpOnly Cookie（见 doc/用户与账号模块设计.md 四）。
 */
public record LoginResponse(
        String accessToken,
        long expiresIn,
        UserProfileResponse user) {
}
