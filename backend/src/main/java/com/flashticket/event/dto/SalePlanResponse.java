package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 售票计划视图（雪花 ID 以字符串传输）。
 */
public record SalePlanResponse(
        String id,
        String tierId,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt,
        String saleStatus,
        Long saleVersion,
        LocalDateTime pausedAt,
        String pauseReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
