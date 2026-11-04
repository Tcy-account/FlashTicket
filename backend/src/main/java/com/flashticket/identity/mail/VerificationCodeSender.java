package com.flashticket.identity.mail;

import com.flashticket.identity.enums.VerifyCodePurpose;

/**
 * 邮箱验证码发送抽象。
 *
 * <p>M01 采用「受控本地邮件模拟模式」（日志输出验证码，见 doc/开发计划 M01 验收），
 * 后续接入真实 SMTP 时新增 {@code SmtpVerificationCodeSender} 实现并替换即可，
 * 业务侧不感知发送细节。
 */
public interface VerificationCodeSender {

    /**
     * 发送验证码邮件。
     *
     * @param email   目标邮箱
     * @param code    6 位数字验证码
     * @param purpose 验证码用途（注册 / 找回密码 / 换绑邮箱）
     */
    void send(String email, String code, VerifyCodePurpose purpose);
}
