package com.flashticket.payment.enums;

/**
 * 支付执行尝试状态（表 ft_payment_attempt.status），见 doc/模拟支付与退款模块.md 四。
 *
 * <p>UNKNOWN 表示暂时无法判断结果、需要恢复查询，V1 同步模拟渠道暂不产生该状态。
 */
public enum PaymentAttemptStatus {
    /** 尝试已登记。 */
    CREATED,
    /** 模拟渠道处理中。 */
    PROCESSING,
    /** 本次尝试最终确认成功。 */
    SUCCEEDED,
    /** 本次尝试确定失败。 */
    FAILED,
    /** 本次尝试被合法关闭。 */
    CLOSED,
    /** 结果暂无法判断，需恢复。 */
    UNKNOWN
}
