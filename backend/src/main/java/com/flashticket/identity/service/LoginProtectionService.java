package com.flashticket.identity.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败保护（Redis）：连续密码错误 5 次触发 10 分钟临时锁定。
 *
 * <p>规则（见 doc/用户与账号模块设计.md 3.3）：
 * <ul>
 *   <li>错误计数窗口 15 分钟；</li>
 *   <li>连续 5 次错误 → 锁定 10 分钟；</li>
 *   <li>登录成功清除计数与锁定。</li>
 * </ul>
 *
 * <p>仅对「已存在的账号」计数，未知账号统一返回错误信息不计数（防账号枚举）。
 */
@Service
public class LoginProtectionService {

    private static final String FAIL_KEY_PREFIX = "auth:login:fail:";
    private static final String LOCK_KEY_PREFIX = "auth:login:lock:";

    private final StringRedisTemplate redis;
    private final int maxFail;
    private final Duration lockDuration;
    private final Duration failWindow;

    public LoginProtectionService(StringRedisTemplate redis,
                                  @Value("${flashticket.auth.login-max-fail}") int maxFail,
                                  @Value("${flashticket.auth.login-lock-duration}") Duration lockDuration,
                                  @Value("${flashticket.auth.login-fail-window}") Duration failWindow) {
        this.redis = redis;
        this.maxFail = maxFail;
        this.lockDuration = lockDuration;
        this.failWindow = failWindow;
    }

    /** 是否处于临时锁定。 */
    public boolean isLocked(String account) {
        return Boolean.TRUE.equals(redis.hasKey(lockKey(account)));
    }

    /** 校验锁定态，锁定则抛 423。 */
    public void assertNotLocked(String account) {
        if (isLocked(account)) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
    }

    /** 记录一次密码错误；达到阈值触发锁定。 */
    public void recordFailure(String account) {
        String failKey = failKey(account);
        Long attempts = redis.opsForValue().increment(failKey);
        if (attempts != null && attempts == 1) {
            redis.expire(failKey, failWindow);
        }
        if (attempts != null && attempts >= maxFail) {
            redis.opsForValue().set(lockKey(account), "1", lockDuration);
            redis.delete(failKey);
        }
    }

    /** 登录成功：清除错误计数与锁定。 */
    public void clear(String account) {
        redis.delete(failKey(account));
        redis.delete(lockKey(account));
    }

    private String failKey(String account) {
        return FAIL_KEY_PREFIX + account;
    }

    private String lockKey(String account) {
        return LOCK_KEY_PREFIX + account;
    }
}
