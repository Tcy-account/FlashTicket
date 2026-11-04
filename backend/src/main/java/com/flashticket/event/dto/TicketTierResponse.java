package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 票档视图（雪花 ID 以字符串传输）。嵌套售票计划（可为空，表示尚未配置）。
 */
public record TicketTierResponse(
        String id,
        String sessionId,
        String name,
        String tierType,
        Long unitPriceCent,
        Integer initialQuantity,
        Integer minPurchase,
        Integer maxPurchase,
        Integer perUserLimit,
        String description,
        Integer sortOrder,
        Long version,
        SalePlanResponse salePlan,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
