package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 演出活动视图（雪花 ID 以字符串传输）。
 */
public record EventResponse(
        String id,
        String categoryId,
        String title,
        String subtitle,
        String coverUrl,
        String description,
        String detailContent,
        String organizerName,
        String attendanceNotice,
        String refundPolicy,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
