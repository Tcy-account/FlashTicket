package com.flashticket.entitlement.dto;

import java.time.LocalDateTime;

/**
 * 票务权益响应（雪花 ID 一律 String 传输）。
 *
 * @param id             权益 ID
 * @param entitlementNo  票务凭证编号
 * @param orderId        所属订单 ID
 * @param orderItemId    所属订单明细 ID
 * @param userId         持有人用户 ID
 * @param eventId        活动 ID
 * @param eventTitle     活动名称（成交快照）
 * @param sessionId      场次 ID
 * @param sessionStartAt 演出开始时间（成交快照）
 * @param venueName      场馆名称（成交快照）
 * @param tierId         票档 ID
 * @param tierName       票档名称（成交快照）
 * @param ticketIndex    本明细内第几张票
 * @param status         权益状态
 * @param issuedAt       出票时间（UTC）
 * @param createdAt      创建时间（UTC）
 */
public record EntitlementResponse(
        String id,
        String entitlementNo,
        String orderId,
        String orderItemId,
        String userId,
        String eventId,
        String eventTitle,
        String sessionId,
        LocalDateTime sessionStartAt,
        String venueName,
        String tierId,
        String tierName,
        int ticketIndex,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime createdAt) {
}
