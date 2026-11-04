package com.flashticket.coupon.dto;

/**
 * 领券结果（同步领券，含发放成功的用户券）。
 */
public record ClaimResult(
        String requestId,
        String status,
        Long userCouponId,
        String failReason,
        UserCouponResponse coupon) {
}
