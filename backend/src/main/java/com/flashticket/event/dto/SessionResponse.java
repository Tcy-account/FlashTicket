package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 演出场次视图（雪花 ID 以字符串传输）。
 */
public record SessionResponse(
        String id,
        String eventId,
        String venueId,
        String name,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime entryStartAt,
        Integer saleCapacity,
        String status,
        LocalDateTime postponedAt,
        LocalDateTime cancelledAt,
        String changeReason,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
