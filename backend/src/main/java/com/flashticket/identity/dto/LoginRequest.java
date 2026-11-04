package com.flashticket.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求：用户名或已验证邮箱 + 密码。
 */
public record LoginRequest(
        @NotBlank(message = "请输入账号") String account,
        @NotBlank(message = "请输入密码") String password) {
}
