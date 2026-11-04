package com.flashticket.coupon.dto;

import java.time.LocalDateTime;

/**
 * 营销活动详情响应。
 */
public record CampaignResponse(
        Long id,
        String name,
        String type,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Long createdBy,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long version,
        LocalDateTime createdAt) {
}
