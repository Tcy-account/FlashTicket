package com.flashticket.risk.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 多维限流服务（M15-A）：基于 Redis Lua 滑动窗口计数的原子限流。
 *
 * <p>原子性由 Lua 脚本在 Redis 服务端保证：清理窗口外记录 → 计数 → 判断 →
 * 追加请求，单次执行，多实例共享同一限流额度，不存在并发超发令牌问题。
 *
 * <p>关键原则：风控限流是<b>保护</b>而非正确性边界。Redis 不可用时 fail-open
 * 放行，业务的防超卖/防超领仍由 MySQL 权威账本与唯一约束兜底。
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final String KEY_PREFIX = "ft:risk:rate:";

    /** 滑动窗口计数脚本：KEYS[1]=key，ARGV=窗口ms/上限/当前ms/成员。返回 1 放行、0 超限。 */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local windowMs = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local nowMs = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, nowMs - windowMs)
            local count = redis.call('ZCARD', key)
            if count >= limit then
                return 0
            end
            redis.call('ZADD', key, nowMs, member)
            redis.call('PEXPIRE', key, windowMs)
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int purchaseLimit;
    private final Duration purchaseWindow;
    private final int claimLimit;
    private final Duration claimWindow;
    private final int payLimit;
    private final Duration payWindow;
    private final int queryLimit;
    private final Duration queryWindow;

    public RateLimitService(StringRedisTemplate redis,
                            @Value("${flashticket.risk.rate-limit.purchase-limit:1}") int purchaseLimit,
                            @Value("${flashticket.risk.rate-limit.purchase-window:5s}") Duration purchaseWindow,
                            @Value("${flashticket.risk.rate-limit.claim-limit:1}") int claimLimit,
                            @Value("${flashticket.risk.rate-limit.claim-window:5s}") Duration claimWindow,
                            @Value("${flashticket.risk.rate-limit.pay-limit:1}") int payLimit,
                            @Value("${flashticket.risk.rate-limit.pay-window:5s}") Duration payWindow,
                            @Value("${flashticket.risk.rate-limit.query-limit:120}") int queryLimit,
                            @Value("${flashticket.risk.rate-limit.query-window:60s}") Duration queryWindow) {
        this.redis = redis;
        this.purchaseLimit = purchaseLimit;
        this.purchaseWindow = purchaseWindow;
        this.claimLimit = claimLimit;
        this.claimWindow = claimWindow;
        this.payLimit = payLimit;
        this.payWindow = payWindow;
        this.queryLimit = queryLimit;
        this.queryWindow = queryWindow;
    }

    /** 抢票限流：同账号同票档窗口内最多 N 个新请求。 */
    public void checkPurchase(Long userId, Long tierId) {
        check(KEY_PREFIX + "user:" + userId + ":purchase:" + tierId, purchaseLimit, purchaseWindow);
    }

    /** 领券限流：同账号同模板窗口内最多 N 个新请求。 */
    public void checkClaim(Long userId, Long templateId) {
        check(KEY_PREFIX + "user:" + userId + ":claim:" + templateId, claimLimit, claimWindow);
    }

    /** 支付尝试限流：同订单窗口内最多 N 次新尝试。 */
    public void checkPay(Long orderId) {
        check(KEY_PREFIX + "order:" + orderId + ":pay", payLimit, payWindow);
    }

    /** 公开查询限流：同 IP 窗口内最多 N 次。 */
    public void checkQueryIp(String ip) {
        check(KEY_PREFIX + "ip:" + ip + ":query", queryLimit, queryWindow);
    }

    /* ---------- 内部 ---------- */

    private void check(String key, int limit, Duration window) {
        if (!tryAcquire(key, limit, window)) {
            throw new BizException(ErrorCode.RISK_RATE_LIMITED);
        }
    }

    /** 尝试获取一个限流名额；Redis 不可用时 fail-open（放行）。 */
    private boolean tryAcquire(String key, int limit, Duration window) {
        try {
            Long result = redis.execute(SLIDING_WINDOW, List.of(key),
                    String.valueOf(window.toMillis()),
                    String.valueOf(limit),
                    String.valueOf(System.currentTimeMillis()),
                    UUID.randomUUID().toString());
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("限流 Redis 不可用，放行 key={}: {}", key, e.getMessage());
            return true;
        }
    }
}
