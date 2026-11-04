package com.flashticket.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 取消活动请求（原因必填，写入 audit 与 ft_event.cancel_reason）。
 */
public record EventCancelRequest(
        @NotBlank(message = "取消原因不能为空")
        @Size(max = 500, message = "取消原因最长 500 字符")
        String reason) {
}
