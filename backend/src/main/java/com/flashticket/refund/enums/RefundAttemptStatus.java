package com.flashticket.refund.enums;

/**
 * 退款执行尝试状态（表 ft_refund_attempt.status），见 doc/全局逻辑ER模型.md 7.2。
 */
public enum RefundAttemptStatus {
    /** 尝试已创建。 */
    CREATED,
    /** 模拟退款处理中。 */
    PROCESSING,
    /** 本次执行成功。 */
    SUCCEEDED,
    /** 确认本次执行失败。 */
    FAILED,
    /** 执行结果暂时未知。 */
    UNKNOWN,
    /** 本次执行终止。 */
    CLOSED
}
