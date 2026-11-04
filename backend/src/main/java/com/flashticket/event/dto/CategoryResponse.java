package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 分类视图（雪花 ID 以字符串传输）。
 */
public record CategoryResponse(
        String id,
        String code,
        String name,
        String description,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
