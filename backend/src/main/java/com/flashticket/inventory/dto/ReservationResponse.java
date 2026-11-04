package com.flashticket.inventory.dto;

import java.time.LocalDateTime;

/**
 * 库存预占记录视图（表 ft_reservation）。
 */
public record ReservationResponse(
        String id,
        String requestId,
        String tierId,
        String userId,
        Integer quantity,
        String status,
        LocalDateTime expireAt,
        Long version,
        LocalDateTime createdAt) {
}
