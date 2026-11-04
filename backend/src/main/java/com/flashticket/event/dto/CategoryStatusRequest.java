package com.flashticket.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 启用/停用分类请求。
 */
public record CategoryStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "状态须为 ENABLED 或 DISABLED")
        String status) {
}
