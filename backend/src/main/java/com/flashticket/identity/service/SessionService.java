package com.flashticket.identity.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 登录会话领域服务（Redis）：Refresh Token 存储、轮换与撤销。
 *
 * <p>存储模型（见 doc/用户与账号模块设计.md 四）：
 * <ul>
 *   <li>{@code auth:session:{sessionId}} → HASH { userId, exp, refreshHash, device }，TTL 7 天；</li>
 *   <li>{@code auth:refresh:{refreshHash}} → sessionId，TTL = 剩余有效期（绝对 7 天，不滑动）；</li>
 *   <li>{@code auth:user:sessions:{userId}} → LIST sessionId，用于多设备上限与全量下线。</li>
 * </ul>
 *
 * <p>Refresh Token 为随机不透明令牌，Redis 只存其 SHA-256 哈希；
 * 每次刷新原子轮换（Lua），旧令牌立即作废。
 */
@Service
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String USER_SESSIONS_KEY_PREFIX = "auth:user:sessions:";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 原子轮换脚本：校验旧令牌→删除→写入新令牌，返回旧 sessionId；失败返回空串。 */
    private static final DefaultRedisScript<String> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local oldKey = KEYS[1]
            local newKey = KEYS[2]
            local sessionKey = KEYS[3]
            local sid = ARGV[1]
            local ttlMs = ARGV[2]
            local newHash = ARGV[3]
            local storedSid = redis.call('GET', oldKey)
            if not storedSid then
                return ''
            end
            if storedSid ~= sid then
                return ''
            end
            if redis.call('EXISTS', sessionKey) == 0 then
                return ''
            end
            redis.call('DEL', oldKey)
            redis.call('SET', newKey, sid, 'PX', ttlMs)
            redis.call('HSET', sessionKey, 'refreshHash', newHash)
            return storedSid
            """, String.class);

    private final StringRedisTemplate redis;
    private final Duration refreshTtl;
    private final int maxSessions;

    public SessionService(StringRedisTemplate redis,
                          @Value("${flashticket.jwt.refresh-token-ttl}") Duration refreshTtl,
                          @Value("${flashticket.auth.max-sessions}") int maxSessions) {
        this.redis = redis;
        this.refreshTtl = refreshTtl;
        this.maxSessions = maxSessions;
    }

    /** 会话签发结果。 */
    public record SessionIssued(String sessionId, String refreshToken) {
    }

    /** 轮换结果。 */
    public record RotatedSession(String sessionId, String refreshToken) {
    }

    /** 创建一个新会话并返回 sessionId + refreshToken（调用方负责签发 Access Token）。 */
    public SessionIssued createSession(Long userId, String device) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = generateToken();
        String refreshHash = sha256(refreshToken);
        long exp = System.currentTimeMillis() + refreshTtl.toMillis();

        String sessionKey = sessionKey(sessionId);
        redis.opsForHash().putAll(sessionKey, Map.of(
                "userId", String.valueOf(userId),
                "exp", String.valueOf(exp),
                "refreshHash", refreshHash,
                "device", device == null ? "" : device));
        redis.expire(sessionKey, refreshTtl);
        redis.opsForValue().set(refreshKey(refreshHash), sessionId, refreshTtl);

        // 多设备上限：新会话入队头，超出则从队尾淘汰最旧会话
        String userSessionsKey = userSessionsKey(userId);
        redis.opsForList().leftPush(userSessionsKey, sessionId);
        redis.expire(userSessionsKey, refreshTtl);
        Long size = redis.opsForList().size(userSessionsKey);
        for (long i = (size == null ? 0 : size); i > maxSessions; i--) {
            String oldest = redis.opsForList().rightPop(userSessionsKey);
            if (oldest == null) {
                break;
            }
            revokeSession(oldest);
        }

        return new SessionIssued(sessionId, refreshToken);
    }

    /** 校验会话是否仍活跃（用于 JWT 过滤器的即时撤销检查）。 */
    public boolean isActive(String sessionId) {
        return Boolean.TRUE.equals(redis.hasKey(sessionKey(sessionId)));
    }

    /** 读取会话 userId（用于刷新时加载用户与角色）。 */
    public Long getUserId(String sessionId) {
        Object v = redis.opsForHash().get(sessionKey(sessionId), "userId");
        return v == null ? null : Long.valueOf(v.toString());
    }

    /** 轮换 Refresh Token：旧令牌一次性作废，返回新令牌。 */
    public RotatedSession rotateRefreshToken(String refreshToken) {
        String oldHash = sha256(refreshToken);
        String oldKey = refreshKey(oldHash);

        String sessionId = redis.opsForValue().get(oldKey);
        if (sessionId == null) {
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        String sessionKey = sessionKey(sessionId);

        Object expObj = redis.opsForHash().get(sessionKey, "exp");
        long remaining = refreshTtl.toMillis();
        if (expObj != null) {
            remaining = Long.parseLong(expObj.toString()) - System.currentTimeMillis();
        }
        if (remaining <= 0) {
            revokeSession(sessionId);
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        String newRefreshToken = generateToken();
        String newHash = sha256(newRefreshToken);

        String result = redis.execute(ROTATE_SCRIPT,
                List.of(oldKey, refreshKey(newHash), sessionKey),
                sessionId, String.valueOf(remaining), newHash);

        if (result == null || result.isEmpty()) {
            // 并发轮换：旧令牌已被消费
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        return new RotatedSession(sessionId, newRefreshToken);
    }

    /** 撤销单个会话（退出当前设备）。 */
    public void revokeSession(String sessionId) {
        String sessionKey = sessionKey(sessionId);
        Map<Object, Object> entry = redis.opsForHash().entries(sessionKey);
        if (entry.isEmpty()) {
            return;
        }
        Object refreshHash = entry.get("refreshHash");
        if (refreshHash != null) {
            redis.delete(refreshKey(refreshHash.toString()));
        }
        redis.delete(sessionKey);
        Object userId = entry.get("userId");
        if (userId != null) {
            redis.opsForList().remove(userSessionsKey(Long.valueOf(userId.toString())), 0, sessionId);
        }
    }

    /** 撤销该用户全部会话（退出全部设备 / 禁用 / 重置密码）。 */
    public void revokeAllSessions(Long userId) {
        String userSessionsKey = userSessionsKey(userId);
        List<String> sessionIds = redis.opsForList().range(userSessionsKey, 0, -1);
        if (sessionIds != null) {
            sessionIds.forEach(this::revokeSession);
        }
        redis.delete(userSessionsKey);
    }

    /** 撤销该用户除指定会话外的其余会话（修改密码后保留当前设备）。 */
    public void revokeOtherSessions(Long userId, String keepSessionId) {
        String userSessionsKey = userSessionsKey(userId);
        List<String> sessionIds = redis.opsForList().range(userSessionsKey, 0, -1);
        if (sessionIds != null) {
            sessionIds.stream()
                    .filter(sid -> !sid.equals(keepSessionId))
                    .forEach(this::revokeSession);
        }
    }

    /* ---------- 工具 ---------- */

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String refreshKey(String refreshHash) {
        return REFRESH_KEY_PREFIX + refreshHash;
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }
}
