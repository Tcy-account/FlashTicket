package com.flashticket.order.dto;

import java.time.LocalDateTime;

/**
 * 订单操作流水响应（雪花 ID 一律 String 传输）。
 *
 * @param operation   操作类型 ORDER_CREATED/USER_CANCELLED/TIMEOUT_CLOSED
 * @param fromStatus  操作前状态（创建事件为 null）
 * @param toStatus    操作后状态
 * @param actorId     操作主体（系统操作为 null）
 * @param reason      关单原因
 * @param createdAt   操作时间（UTC）
 */
public record OrderOperationResponse(
        String id,
        String orderId,
        String operation,
        String fromStatus,
        String toStatus,
        String actorId,
        String reason,
        String operationId,
        LocalDateTime createdAt) {
}
