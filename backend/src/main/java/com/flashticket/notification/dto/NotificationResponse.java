package com.flashticket.notification.dto;

import java.time.LocalDateTime;

/**
 * 用户消息响应（id 为接收记录 id，同时作为游标分页与单条已读的定位键）。
 */
public record NotificationResponse(
        String id,
        String type,
        String title,
        String content,
        String bizType,
        String bizId,
        boolean read,
        LocalDateTime receivedAt
) {
}
