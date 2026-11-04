package com.flashticket.identity.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.identity.enums.VerifyCodePurpose;
import com.flashticket.identity.mail.VerificationCodeSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 邮箱验证码领域服务：生成、发送、原子核销。
 *
 * <p>规则（见 doc/用户与账号模块设计.md 3.1）：
 * <ul>
 *   <li>验证码 6 位数字，有效期 5 分钟（TTL 300s）；</li>
 *   <li>同一邮箱 60 秒内最多发送一次；</li>
 *   <li>同一验证码最多尝试 5 次，超次作废需重新获取；</li>
 *   <li>用途隔离：注册 / 找回密码 / 换绑邮箱互不通用。</li>
 * </ul>
 */
@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);

    private static final String CODE_KEY_PREFIX = "auth:verify:code:";
    private static final String ATTEMPT_KEY_PREFIX = "auth:verify:attempt:";
    private static final String SEND_LIMIT_KEY_PREFIX = "auth:verify:send:";

    private static final Duration CODE_TTL = Duration.ofSeconds(300);
    private static final Duration SEND_LIMIT_TTL = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 原子核销脚本：校验 + 错误计数 + 成功删除，一次 Redis 往返完成，避免并发下
     * 同一验证码被重复核销或错误计数竞态。
     *
     * <p>返回：1=核销成功；-1=验证码不存在或已过期；-2=尝试次数超限；-3=验证码不匹配。
     */
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            local codeKey = KEYS[1]
            local attemptKey = KEYS[2]
            local input = ARGV[1]
            local maxAttempts = tonumber(ARGV[2])

            local stored = redis.call('GET', codeKey)
            if not stored then
                return -1
            end
            if stored ~= input then
                local attempts = redis.call('INCR', attemptKey)
                redis.call('EXPIRE', attemptKey, 300)
                if attempts >= maxAttempts then
                    return -2
                end
                return -3
            end
            redis.call('DEL', codeKey)
            redis.call('DEL', attemptKey)
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final VerificationCodeSender sender;

    public VerificationCodeService(StringRedisTemplate redisTemplate, VerificationCodeSender sender) {
        this.redisTemplate = redisTemplate;
        this.sender = sender;
    }

    /** 发送验证码（含 60 秒发送限频）。 */
    public void sendCode(String email, VerifyCodePurpose purpose) {
        email = normalizeEmail(email);
        String sendLimitKey = sendLimitKey(email, purpose);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(sendLimitKey, "1", SEND_LIMIT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(ErrorCode.AUTH_VERIFY_CODE_SENT_LIMIT);
        }

        String code = generateCode();
        try {
            sender.send(email, code, purpose);
        } catch (Exception ex) {
            // 发送失败不落 Redis，避免留下无法继续的验证码状态；限频标记保留 60s 防刷。
            log.warn("邮箱验证码发送失败 email={} purpose={}", email, purpose, ex);
            throw new BizException(ErrorCode.AUTH_EMAIL_SEND_FAILED);
        }

        redisTemplate.opsForValue().set(codeKey(email, purpose), code, CODE_TTL);
        redisTemplate.delete(attemptKey(email, purpose));
    }

    /** 校验并核销验证码（成功后一次性作废）。 */
    public void verify(String email, VerifyCodePurpose purpose, String inputCode) {
        email = normalizeEmail(email);
        Long result = redisTemplate.execute(VERIFY_SCRIPT,
                List.of(codeKey(email, purpose), attemptKey(email, purpose)),
                inputCode, String.valueOf(MAX_ATTEMPTS));

        int code = result == null ? -1 : result.intValue();
        if (code == 1) {
            return;
        }
        if (code == -2) {
            throw new BizException(ErrorCode.AUTH_VERIFY_CODE_ATTEMPTS_EXCEEDED);
        }
        throw new BizException(ErrorCode.AUTH_VERIFY_CODE_INVALID);
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** 邮箱规范化：trim + 小写，保证发送与核销使用同一 Redis key。 */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String codeKey(String email, VerifyCodePurpose purpose) {
        return CODE_KEY_PREFIX + purpose.name() + ":" + email;
    }

    private String attemptKey(String email, VerifyCodePurpose purpose) {
        return ATTEMPT_KEY_PREFIX + purpose.name() + ":" + email;
    }

    private String sendLimitKey(String email, VerifyCodePurpose purpose) {
        return SEND_LIMIT_KEY_PREFIX + purpose.name() + ":" + email;
    }
}
