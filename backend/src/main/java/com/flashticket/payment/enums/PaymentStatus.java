package com.flashticket.payment.enums;

/**
 * 支付业务单状态（表 ft_payment.status），见 doc/模拟支付与退款模块.md 四。
 *
 * <p>一次支付尝试失败不等于整张支付业务单失败：V1 模拟渠道确定性成功，
 * 状态机保留完整语义以支撑后续多渠道扩展。SUCCEEDED/FAILED/CLOSED 为终态。
 */
public enum PaymentStatus {
    /** 支付单已创建，尚未执行。 */
    CREATED,
    /** 存在正在执行的支付尝试。 */
    PROCESSING,
    /** 支付最终确认成功。 */
    SUCCEEDED,
    /** 支付业务确定失败且不再允许本支付单重试。 */
    FAILED,
    /** 订单关闭或支付过期。 */
    CLOSED
}
