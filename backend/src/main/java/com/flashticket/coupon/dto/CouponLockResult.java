package com.flashticket.coupon.dto;

/**
 * 锁券结果（建单事务内锁券 + 优惠计算，供订单快照）。
 */
public record CouponLockResult(
        Long userCouponId,
        Long couponTemplateId,
        long discountAmount,
        String promotionSnapshot) {
}
