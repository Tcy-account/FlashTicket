package com.flashticket.notification.dto;

/**
 * 通知业务事件（P0-B 写入 Outbox 的载荷）。
 *
 * <p>业务侧在终态事务内构造：eventId 同时作为 Outbox 去重键与 Inbox 幂等键；
 * eventType 决定通知类型、标题、正文与跳转类型（见 {@code NotificationEventType}）；
 * bizId 为跳转业务 ID（订单/支付/退款/抢票请求的雪花字符串）。
 */
public record NotificationEvent(
        String eventId,
        String eventType,
        Long userId,
        String bizId
) {
}
