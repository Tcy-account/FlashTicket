package com.flashticket.notification.dto;

import java.time.LocalDateTime;

/**
 * 通知发送失败工单（后台重试入口，来源为 Outbox 中尝试过但未发送成功的事件）。
 */
public record NotificationFailureResponse(
        String id,
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        int attemptCount,
        String lastError,
        LocalDateTime createdAt
) {
}
