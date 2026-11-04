package com.flashticket.coupon.dto;

import java.time.LocalDateTime;

/**
 * 优惠券模板详情响应（含发行额度概览）。
 */
public record CouponTemplateResponse(
        Long id,
        Long campaignId,
        String name,
        String type,
        Long thresholdCent,
        Long discountCent,
        Integer discountRate,
        Long maxDiscountCent,
        Integer totalQuantity,
        Integer perUserLimit,
        LocalDateTime claimStartAt,
        LocalDateTime claimEndAt,
        String validityType,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer validDays,
        String applicableScope,
        Long scopeRefId,
        String status,
        Long version,
        Integer quotaAvailable,
        Integer quotaIssued,
        LocalDateTime createdAt) {
}
