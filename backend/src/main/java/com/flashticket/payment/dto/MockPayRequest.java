package com.flashticket.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 模拟执行支付请求。
 *
 * @param attemptRequestId 客户端支付幂等请求号（重复提交只返回原结果，不重复确认）
 */
public record MockPayRequest(
        @NotBlank @Size(max = 64) String attemptRequestId) {
}
