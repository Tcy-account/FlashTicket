package com.flashticket.order.enums;

/**
 * 订单操作流水类型（表 ft_order_operation.operation），见 doc/订单与交易状态机模块.md 九。
 *
 * <p>用于订单状态变更审计。M09 落地创建、用户取消、超时关单；M10 落地支付确认；
 * M11 落地退款申请与退款成功（订单主状态保持 PAID，退款详情以 ft_refund 为准）。
 */
public enum OrderOperationType {
    /** 抢票成功创建订单。 */
    ORDER_CREATED,
    /** 用户主动取消。 */
    USER_CANCELLED,
    /** 支付超时系统关单。 */
    TIMEOUT_CLOSED,
    /** 模拟支付确认成功。 */
    PAYMENT_CONFIRMED,
    /** 用户申请退款（PAID → PAID）。 */
    REFUND_REQUESTED,
    /** 退款成功，权益撤销（PAID → PAID）。 */
    REFUND_SUCCEEDED
}
