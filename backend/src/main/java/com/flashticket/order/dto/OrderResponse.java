package com.flashticket.order.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情/列表项（含明细快照）。
 *
 * <p>金额字段均为整数分；雪花 ID 一律 String 传输。items 在列表场景可能为空列表
 * （详情接口聚合明细），见 OrderService。
 */
public record OrderResponse(
        String id,
        String orderNo,
        String requestId,
        String userId,
        String status,
        Long originalAmountCent,
        Long discountAmountCent,
        Long payableAmountCent,
        Long paidAmountCent,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        LocalDateTime closedAt,
        String closeReason,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items) {
}
