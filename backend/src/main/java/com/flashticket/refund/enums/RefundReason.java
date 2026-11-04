package com.flashticket.refund.enums;

/**
 * 退款原因（表 ft_refund.reason_code），见 doc/模拟支付与退款模块.md 6.2。
 *
 * <p>V1 支持个人原因、演出延期、演出取消、其他（填写说明）四类。
 */
public enum RefundReason {
    /** 个人原因申请退款。 */
    PERSONAL,
    /** 演出延期。 */
    EVENT_POSTPONED,
    /** 演出取消。 */
    EVENT_CANCELLED,
    /** 其他原因（填写说明）。 */
    OTHER
}
