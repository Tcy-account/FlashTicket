package com.flashticket.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 启用/禁用账号请求。
 */
public record UpdateUserStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "状态须为 ACTIVE 或 DISABLED")
        String status) {
}
