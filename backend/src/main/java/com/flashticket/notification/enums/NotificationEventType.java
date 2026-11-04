package com.flashticket.notification.enums;

/**
 * 通知业务事件类型（M14 P0-B）。
 *
 * <p>业务侧在终态事务内把「事件类型 + 接收人 + 业务 ID」写进 Outbox，
 * 通知消费者据此渲染固定标题/正文并落库。标题正文为业务发生时的固定文案
 * （P0 不插值业务字段），bizType 决定前端跳转目标。
 */
public enum NotificationEventType {

    PURCHASE_SUCCESS(NotificationType.TRANSACTION, "抢票成功",
            "已成功预占门票，请在支付期限前完成支付", "ORDER"),
    PURCHASE_FAILED(NotificationType.TRANSACTION, "抢票失败",
            "本次未抢到门票，可关注后续余票", "PURCHASE"),
    PAYMENT_SUCCESS(NotificationType.TRANSACTION, "支付成功",
            "订单支付已确认，可在我的订单中查看", "ORDER"),
    ORDER_CLOSED(NotificationType.TRANSACTION, "订单已关闭",
            "订单因超时未支付而关闭，如有需要可重新购买", "ORDER"),
    REFUND_REQUESTED(NotificationType.TRANSACTION, "退款申请已受理",
            "您的退款申请已提交，等待财务审核", "REFUND"),
    REFUND_REJECTED(NotificationType.TRANSACTION, "退款申请被拒绝",
            "您的退款申请未通过审核，请查看详情", "REFUND"),
    REFUND_SUCCESS(NotificationType.TRANSACTION, "退款成功",
            "您的退款已完成，请留意到账", "REFUND");

    private final NotificationType type;
    private final String title;
    private final String content;
    private final String bizType;

    NotificationEventType(NotificationType type, String title, String content, String bizType) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.bizType = bizType;
    }

    public NotificationType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public String bizType() {
        return bizType;
    }
}
