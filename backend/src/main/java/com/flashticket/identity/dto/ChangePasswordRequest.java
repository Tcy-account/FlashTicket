package com.flashticket.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求（已登录，验证旧密码）。
 */
public record ChangePasswordRequest(
        @NotBlank(message = "请输入当前密码") String oldPassword,
        @NotBlank(message = "请输入新密码")
        @Size(min = 12, max = 64, message = "密码长度需为 12-64 位") String newPassword) {
}
