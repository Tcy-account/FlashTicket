package com.flashticket.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户退款申请请求（POST /api/v1/orders/{orderId}/refunds）。
 *
 * @param refundRequestId 客户端幂等请求号（重复提交只返回原退款单，不重复创建）
 * @param reasonCode      退款原因 PERSONAL/EVENT_POSTPONED/EVENT_CANCELLED/OTHER
 * @param reasonDetail    补充说明（OTHER 时建议填写）
 */
public record RefundRequest(
        @NotBlank @Size(max = 64) String refundRequestId,
        @NotBlank @Pattern(regexp = "PERSONAL|EVENT_POSTPONED|EVENT_CANCELLED|OTHER")
        String reasonCode,
        @Size(max = 500) String reasonDetail) {
}
