package com.flashticket.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 延期场次请求（原因必填）。
 */
public record SessionPostponeRequest(
        @NotBlank(message = "延期原因不能为空")
        @Size(max = 500, message = "延期原因最长 500 字符")
        String reason) {
}
