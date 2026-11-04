package com.flashticket.identity.mail;

import com.flashticket.identity.enums.VerifyCodePurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地模拟邮件发送：验证码仅输出到后端日志，用于本地开发与联调（M01 基线）。
 *
 * <p>安全说明：正式环境严禁使用本实现——验证码落日志即泄露。接入真实邮件服务时
 * 以 {@code @Profile("prod")} 或配置开关替换为 SMTP 实现，日志实现仅保留在 local/dev。
 */
@Component
public class LogVerificationCodeSender implements VerificationCodeSender {

    private static final Logger log = LoggerFactory.getLogger(LogVerificationCodeSender.class);

    @Override
    public void send(String email, String code, VerifyCodePurpose purpose) {
        log.info("[邮箱验证码·本地模拟] 邮箱={} 用途={} 验证码={} —— 请到后端日志查看验证码完成注册",
                email, purpose, code);
    }
}
