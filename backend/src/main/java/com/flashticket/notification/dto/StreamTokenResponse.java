package com.flashticket.notification.dto;

/**
 * SSE 流连接凭证响应。
 *
 * @param expiresInSeconds 凭证有效期（秒），前端据此在过期前完成连接
 */
public record StreamTokenResponse(
        String token,
        long expiresInSeconds
) {
}
