package com.flashticket.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 暂停销售请求（见 doc/演出活动与场次.md 7.2：运营手动暂停售票）。
 */
public record SalePauseRequest(
        @NotBlank(message = "暂停原因不能为空")
        @Size(max = 500, message = "暂停原因最长 500 字符")
        String reason) {
}
