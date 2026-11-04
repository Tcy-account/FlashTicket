package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 场馆视图（雪花 ID 以字符串传输）。
 */
public record VenueResponse(
        String id,
        String name,
        String countryCode,
        String province,
        String city,
        String district,
        String address,
        String timezone,
        Integer maxCapacity,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
