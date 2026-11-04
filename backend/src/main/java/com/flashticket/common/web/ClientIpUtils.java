package com.flashticket.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析（M15 风控限流维度）。
 *
 * <p>优先读取 X-Forwarded-For 首个地址（反向代理场景），否则回退 remoteAddr。
 * 供风控限流、审计等按 IP 维度识别使用；IP 仅作为限流/审计维度，不单独
 * 等同于恶意用户（共享出口 IP 普遍存在）。
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    /** 从请求解析客户端 IP（X-Forwarded-For 首段，否则 remoteAddr）。 */
    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
