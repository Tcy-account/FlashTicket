package com.flashticket.coupon.enums;

/**
 * 优惠券类型（表 ft_coupon_template.type）。
 * <ul>
 *   <li>FULL_REDUCTION：满减券（P0），满 threshold 减 discount；</li>
 *   <li>PERCENTAGE：折扣券（P1），按折扣基点计算，封顶 maxDiscount。</li>
 * </ul>
 */
public enum CouponType {
    FULL_REDUCTION,
    PERCENTAGE
}
