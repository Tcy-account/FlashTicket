package com.flashticket.coupon.enums;

/**
 * 用户优惠券状态机（表 ft_user_coupon.status）。
 * <pre>
 * AVAILABLE → LOCKED → USED        （支付成功核销）
 *            └───────→ AVAILABLE    （关单返券，券仍有效）
 *            └───────→ EXPIRED      （关单返券，券已过期）
 * AVAILABLE → EXPIRED               （有效期自然结束）
 * AVAILABLE → VOID                  （合规作废）
 * </pre>
 */
public enum UserCouponStatus {
    AVAILABLE,
    LOCKED,
    USED,
    EXPIRED,
    VOID
}
