package com.flashticket.coupon.dto;

import java.time.LocalDateTime;

/**
 * 用户端「可领优惠券」列表项（M13 领券中心展示）。
 *
 * <p>仅包含当前可领（模板 ACTIVE + 活动 PUBLISHED + 领取窗口内 + 额度有余）的券，
 * 供前端领券中心与下单选券页面消费。
 */
public record CouponAvailableResponse(
        Long templateId,
        Long campaignId,
        String campaignName,
        String name,
        String type,
        Long thresholdCent,
        Long discountCent,
        Integer discountRate,
        Long maxDiscountCent,
        Integer totalQuantity,
        Integer availableQuantity,
        Integer perUserLimit,
        LocalDateTime claimStartAt,
        LocalDateTime claimEndAt,
        String validityType,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer validDays,
        String applicableScope,
        Long scopeRefId) {
}
