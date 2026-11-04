package com.flashticket.common.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Refresh Token Cookie 工具（HttpOnly + SameSite=Lax，见 doc/用户与账号模块设计.md 四）。
 *
 * <p>用户端与管理端使用不同 cookie 名，避免同一浏览器下两端登录态互相覆盖。
 */
public final class RefreshCookieHelper {

    private RefreshCookieHelper() {
    }

    public static void write(HttpServletResponse response, String cookieName, String refreshToken, Duration ttl) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(false) // 生产应开启（HTTPS）
                .sameSite("Lax")
                .path("/")
                .maxAge(ttl)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void clear(HttpServletResponse response, String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static String read(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
