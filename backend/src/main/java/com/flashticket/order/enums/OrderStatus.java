package com.flashticket.order.enums;

/**
 * 订单交易状态（表 ft_order.status），见 doc/订单与交易状态机模块.md 四。
 *
 * <p>订单只管理交易生命周期：PENDING_PAYMENT → PAID / CLOSED。退款由独立状态机
 * 控制，不混入订单主状态。M08 只落地 PENDING_PAYMENT，PAID/CLOSED 由 M09/M10 启用。
 */
public enum OrderStatus {
    /** 待支付，门票已预占。 */
    PENDING_PAYMENT,
    /** 支付已确认。 */
    PAID,
    /** 取消或超时关闭（原因见 close_reason）。 */
    CLOSED
}
