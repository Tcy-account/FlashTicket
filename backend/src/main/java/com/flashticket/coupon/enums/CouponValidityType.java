package com.flashticket.coupon.enums;

/**
 * 优惠券有效期类型（表 ft_coupon_template.validity_type）。
 * <ul>
 *   <li>FIXED：固定时间（valid_from ~ valid_until）；</li>
 *   <li>RELATIVE_DAYS：领取后 N 天有效（valid_days）。</li>
 * </ul>
 */
public enum CouponValidityType {
    FIXED,
    RELATIVE_DAYS
}
