package com.flashticket.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 找回密码第二步：验证码 + 新密码重置。
 */
public record ResetPasswordRequest(
        @NotBlank(message = "请输入邮箱") @Email(message = "邮箱格式不正确") String email,
        @NotBlank(message = "请输入验证码") @Pattern(regexp = "\\d{6}", message = "验证码为 6 位数字") String verifyCode,
        @NotBlank(message = "请输入新密码")
        @Size(min = 12, max = 64, message = "密码长度需为 12-64 位") String newPassword) {
}
