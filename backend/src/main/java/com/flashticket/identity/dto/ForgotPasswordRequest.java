package com.flashticket.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 找回密码第一步：向邮箱发送重置验证码。
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "请输入邮箱") @Email(message = "邮箱格式不正确") String email) {
}
