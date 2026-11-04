package com.flashticket.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.messaging.service.OutboxService;
import com.flashticket.notification.dto.NotificationEvent;
import com.flashticket.notification.enums.NotificationEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 通知事件发布器（M14 P0-B）。
 *
 * <p>业务域在终态事务内调用 {@link #publish} 一行把通知事件写进 Outbox
 * （与业务状态同事务提交），由 RocketMQ 异步消费后落库生成站内消息。
 * 标题/正文在消费端按事件类型渲染，业务侧只提供「事件类型 + 接收人 + 业务 ID」。
 */
@Service
public class NotificationEventPublisher {

    /** Outbox 事件类型（同时作为 RocketMQ tag），具体业务事件类型在载荷内。 */
    private static final String EVENT_TYPE = "NOTIFICATION";

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final String notificationTopic;

    public NotificationEventPublisher(OutboxService outboxService,
                                      ObjectMapper objectMapper,
                                      @Value("${rocketmq.notification.topic:ft-notification}") String notificationTopic) {
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.notificationTopic = notificationTopic;
    }

    /**
     * 发布一条通知业务事件（须在调用方业务事务内执行）。
     *
     * @param eventType 通知事件类型（决定标题/正文/跳转类型）
     * @param userId    接收人
     * @param bizId     跳转业务 ID（订单/支付/退款/抢票请求的雪花字符串）
     */
    public void publish(NotificationEventType eventType, Long userId, String bizId) {
        String eventId = UUID.randomUUID().toString();
        NotificationEvent event = new NotificationEvent(eventId, eventType.name(), userId, bizId);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化通知事件失败", e);
        }
        outboxService.append(eventId, notificationTopic, EVENT_TYPE,
                eventType.bizType(), bizId, payload);
    }
}
