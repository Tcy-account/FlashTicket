package com.flashticket.notification.dto;

import java.util.List;

/**
 * 用户消息游标分页结果。
 *
 * @param nextCursor 下一页游标（接收记录 id 字符串；null 表示已无更多）
 * @param unreadCount 当前未读数（随列表一并返回，减少前端额外请求）
 */
public record NotificationListResult(
        List<NotificationResponse> items,
        String nextCursor,
        long unreadCount
) {
}
