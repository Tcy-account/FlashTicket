package com.flashticket.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置员工密码请求（管理员指定新密码，不返回明文）。
 */
public record AdminResetPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 12, max = 64, message = "密码长度须为 12-64 位")
        String newPassword) {
}
