package com.flashticket.messaging.service;

import com.flashticket.messaging.entity.OutboxEvent;
import com.flashticket.messaging.mapper.OutboxEventMapper;
import org.springframework.stereotype.Service;

/**
 * Outbox 事件写入封装（M14）。
 *
 * <p>业务域在终态事务内调用 {@link #append} 一行写入待发布事件，与业务状态
 * 同生共死（同一 MySQL 本地事务）。后台 {@link OutboxPublisher} 定时扫描补发。
 * 相比 M12 在各业务服务里手动 new OutboxEvent + insert，收敛了样板代码。
 */
@Service
public class OutboxService {

    private final OutboxEventMapper outboxMapper;

    public OutboxService(OutboxEventMapper outboxMapper) {
        this.outboxMapper = outboxMapper;
    }

    /**
     * 追加一条待发布事件（须在调用方事务内执行）。
     *
     * @param eventId       事件唯一标识（同时作为消费端 Inbox 幂等键）
     * @param topic         目标 RocketMQ topic
     * @param eventType     事件类型（同时作为消息 tag）
     * @param aggregateType 聚合类型（用于追踪，如 ORDER/REFUND/PURCHASE）
     * @param aggregateId   聚合 ID（雪花字符串）
     * @param payload       事件载荷 JSON（发布器原样转发）
     */
    public void append(String eventId, String topic, String eventType,
                       String aggregateType, String aggregateId, String payload) {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(eventId);
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setTopic(topic);
        outbox.setEventType(eventType);
        outbox.setPayload(payload);
        outbox.setSchemaVersion(1);
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outboxMapper.insert(outbox);
    }
}
