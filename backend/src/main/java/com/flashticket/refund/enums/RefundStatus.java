package com.flashticket.refund.enums;

/**
 * 退款业务单状态（表 ft_refund.status），见 doc/模拟支付与退款模块.md 七。
 *
 * <p>状态机：PENDING_REVIEW → APPROVED / REJECTED / CANCELLED；
 * APPROVED → PROCESSING → SUCCEEDED / FAILED；FAILED 允许重试（复用退款业务单）。
 * V1 同步模拟退款下，审核通过后在同一本地事务内自动执行到 SUCCEEDED，
 * APPROVED 为审核字段持久化后的瞬态（执行结果权威状态以 ft_refund_attempt 记录）。
 */
public enum RefundStatus {
    /** 待审核（用户已申请）。 */
    PENDING_REVIEW,
    /** 审核通过，等待/正在执行。 */
    APPROVED,
    /** 审核拒绝，本次申请终结。 */
    REJECTED,
    /** 退款执行中。 */
    PROCESSING,
    /** 退款成功（逆向交易已确认）。 */
    SUCCEEDED,
    /** 退款最终失败（已确认不能继续自动执行）。 */
    FAILED,
    /** 已撤销（审核前用户主动撤销）。 */
    CANCELLED
}
