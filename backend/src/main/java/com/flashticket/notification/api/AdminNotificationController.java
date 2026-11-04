package com.flashticket.notification.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.messaging.entity.OutboxEvent;
import com.flashticket.messaging.mapper.OutboxEventMapper;
import com.flashticket.notification.dto.NotificationFailureResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台通知失败工单入口（M14）：查询发送失败的通知事件并支持重试。
 *
 * <p>失败定义：Outbox 中 topic=ft-notification 且 attempt_count&gt;0（尝试过但尚未
 * 发送成功）的事件。重试即重置其计数与错误，交由 OutboxPublisher 下次扫描重新发送。
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationController {

    private final OutboxEventMapper outboxMapper;
    private final String notificationTopic;

    public AdminNotificationController(OutboxEventMapper outboxMapper,
                                       @Value("${rocketmq.notification.topic:ft-notification}") String notificationTopic) {
        this.outboxMapper = outboxMapper;
        this.notificationTopic = notificationTopic;
    }

    /** 通知失败工单列表（notification:read）。 */
    @GetMapping("/failures")
    @PreAuthorize("hasAuthority('notification:read')")
    public ApiResponse<List<NotificationFailureResponse>> listFailures() {
        List<OutboxEvent> events = outboxMapper.findFailures(notificationTopic, 100);
        return ApiResponse.ok(events.stream().map(AdminNotificationController::toResponse).toList());
    }

    /** 重试一条失败工单（notification:retry）。 */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('notification:retry')")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        if (outboxMapper.resetForRetry(id) == 0) {
            throw new BizException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return ApiResponse.ok(null);
    }

    private static NotificationFailureResponse toResponse(OutboxEvent e) {
        return new NotificationFailureResponse(
                String.valueOf(e.getId()), e.getEventId(), e.getEventType(),
                e.getAggregateType(), e.getAggregateId(),
                e.getAttemptCount() == null ? 0 : e.getAttemptCount(),
                e.getLastError(), e.getCreatedAt());
    }
}
