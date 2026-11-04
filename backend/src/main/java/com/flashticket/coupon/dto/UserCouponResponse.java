package com.flashticket.coupon.dto;

import java.time.LocalDateTime;

/**
 * 用户优惠券详情响应（含模板摘要，供我的券/下单选券展示）。
 */
public record UserCouponResponse(
        Long id,
        Long templateId,
        Long campaignId,
        String couponNo,
        String status,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        String templateName,
        String type,
        Long thresholdCent,
        Long discountCent,
        Integer discountRate,
        Long maxDiscountCent,
        String applicableScope,
        LocalDateTime usedAt,
        LocalDateTime lockedAt,
        LocalDateTime createdAt) {
}
