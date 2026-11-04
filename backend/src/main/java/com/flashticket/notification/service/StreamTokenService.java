package com.flashticket.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * SSE 流连接凭证（M14 P0-D）。
 *
 * <p>原生 EventSource 不支持自定义 Authorization 请求头，故不能把长期 JWT
 * 放进 URL。方案：登录用户先经 {@code POST /notifications/stream-token}（Bearer
 * 认证）换取短期凭证，再以 {@code GET /notifications/stream?token=...} 建立连接。
 * 凭证短时效（默认 30s）绑定 userId，避免长期 JWT 泄漏到 URL/日志。
 */
@Service
public class StreamTokenService {

    private static final String KEY_PREFIX = "ft:notification:stream:";

    private final StringRedisTemplate redis;
    private final long ttlSeconds;

    public StreamTokenService(StringRedisTemplate redis,
                              @Value("${flashticket.notification.stream-token-ttl-seconds:30}") long ttlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
    }

    /** 为用户签发短期流连接凭证。 */
    public StreamTokenInfo issue(Long userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(KEY_PREFIX + token, String.valueOf(userId), Duration.ofSeconds(ttlSeconds));
        return new StreamTokenInfo(token, ttlSeconds);
    }

    /** 解析凭证得到 userId（不存在/已过期返回 null）。 */
    public Long resolve(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String userId = redis.opsForValue().get(KEY_PREFIX + token);
        if (userId == null) {
            return null;
        }
        return Long.valueOf(userId);
    }

    /** 签发的流凭证信息。 */
    public record StreamTokenInfo(String token, long expiresInSeconds) {
    }
}
