package com.flashticket.order.enums;

/**
 * 订单关单原因（表 ft_order.close_reason），见 doc/订单与交易状态机模块.md 四。
 *
 * <p>取消与超时都归入 CLOSED 终态，用 close_reason 区分原因。M09 落地
 * USER_CANCEL（用户主动取消）与 PAYMENT_TIMEOUT（支付超时系统关单）；
 * EVENT_CANCELLED / SYSTEM_CANCEL 留待活动取消与人工关单等后续里程碑。
 */
public enum CloseReason {
    /** 用户主动取消待支付订单。 */
    USER_CANCEL,
    /** 支付期限到期，系统自动关单。 */
    PAYMENT_TIMEOUT
}
