package com.flashticket.coupon.enums;

/**
 * 优惠券适用范围（表 ft_coupon_template.applicable_scope）。
 * <ul>
 *   <li>ALL：全平台通用；</li>
 *   <li>EVENT：指定演出（scope_ref_id = eventId）；</li>
 *   <li>TIER：指定票档（scope_ref_id = tierId）。</li>
 * </ul>
 */
public enum CouponApplicableScope {
    ALL,
    EVENT,
    TIER
}
