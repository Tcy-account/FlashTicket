package com.flashticket.purchase.dto;

import com.flashticket.order.dto.OrderResponse;

import java.time.LocalDateTime;

/**
 * 抢票请求状态（GET /api/v1/purchases/{requestId} 响应，也是前端排队页轮询依据）。
 *
 * <p>status 为权威状态（QUEUED/PROCESSING/RECOVERING/SUCCEEDED/FAILED/CANCELLED），
 * 由 ft_purchase_request 持久化。SUCCEEDED 时 orderId/order 非空；FAILED 时
 * failReason 说明确定性失败原因。order 为 null 时前端不得展示「抢票成功」。
 */
public record PurchaseStatusResponse(
        String requestId,
        String status,
        String failReason,
        String orderId,
        OrderResponse order,
        Integer attemptCount,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}
