package com.flashticket.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 领券请求（requestId 幂等键由客户端生成）。
 */
public record ClaimRequest(
        @NotBlank(message = "领券请求号不能为空")
        String requestId,

        @NotNull(message = "优惠券模板不能为空")
        Long templateId) {
}
