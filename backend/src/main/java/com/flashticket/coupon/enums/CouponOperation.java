package com.flashticket.coupon.enums;

/**
 * 优惠券审计流水操作类型（表 ft_coupon_ledger.operation）。
 */
public enum CouponOperation {
    /** 发行额度调整 */
    QUOTA_ADJUST,
    /** 发券 */
    CLAIM,
    /** 订单锁券 */
    LOCK,
    /** 支付核销 */
    USE,
    /** 关单返券/释放 */
    RELEASE,
    /** 过期 */
    EXPIRE,
    /** 作废 */
    VOID
}
