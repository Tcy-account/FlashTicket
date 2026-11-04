package com.flashticket.order.dto;

import java.time.LocalDateTime;

/**
 * 订单明细交易快照（见 doc/订单与交易状态机模块.md 八）。
 */
public record OrderItemResponse(
        String id,
        String orderId,
        String eventId,
        String eventTitle,
        String sessionId,
        LocalDateTime sessionStartAt,
        String venueName,
        String tierId,
        String tierName,
        Long unitPriceCent,
        Integer quantity,
        Long amountCent) {
}
