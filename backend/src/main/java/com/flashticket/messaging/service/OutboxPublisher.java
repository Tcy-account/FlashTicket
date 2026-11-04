package com.flashticket.messaging.service;

import com.flashticket.messaging.entity.OutboxEvent;
import com.flashticket.messaging.mapper.OutboxEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Outbox 发布器（M12，M14 泛化）：定时扫描待发布事件补发到 RocketMQ。
 *
 * <p>与业务同事务写入的 Outbox 事件，即使应用在 HTTP 返回后崩溃也不会丢失；
 * 本扫描器周期性领取发布租约（防多实例重复发布）→ 发送 → 标记 SENT / 记录重试。
 * M14 起改为原样转发 payload JSON（不反序列化成具体类型），支持任意事件类型。
 * test profile 下关闭，集成测试通过直接调用发布逻辑或验证 Outbox 落库覆盖。
 */
@Component
@Profile("!test")
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventMapper outboxMapper;
    private final RocketMQPublisher publisher;
    private final int batchSize;
    private final Duration leaseDuration;
    private final Duration retryInterval;

    public OutboxPublisher(OutboxEventMapper outboxMapper,
                           RocketMQPublisher publisher,
                           @Value("${flashticket.messaging.outbox.batch-size:100}") int batchSize,
                           @Value("${flashticket.messaging.outbox.lease-seconds:30}") int leaseSeconds,
                           @Value("${flashticket.messaging.outbox.retry-seconds:5}") int retrySeconds) {
        this.outboxMapper = outboxMapper;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);
        this.retryInterval = Duration.ofSeconds(retrySeconds);
    }

    /** 周期性扫描一批 PENDING 事件并尝试发布。 */
    @Scheduled(fixedDelayString = "${flashticket.messaging.outbox.scan-delay-ms:1000}",
            initialDelayString = "${flashticket.messaging.outbox.scan-delay-ms:1000}")
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<OutboxEvent> pending = outboxMapper.findPending(now, batchSize);
        for (OutboxEvent event : pending) {
            publishOne(event, now);
        }
    }

    private void publishOne(OutboxEvent event, LocalDateTime now) {
        String leaseOwner = UUID.randomUUID().toString();
        LocalDateTime leaseUntil = now.plus(leaseDuration);
        if (outboxMapper.acquireLease(event.getId(), leaseOwner, leaseUntil) == 0) {
            return; // 其他发布实例已领取，跳过
        }
        try {
            // M14 泛化：原样转发 payload，发布器不关心具体事件类型
            publisher.sendRaw(event.getTopic(), event.getEventType(), event.getPayload());
            outboxMapper.markSent(event.getId(), leaseOwner, now);
        } catch (Exception e) {
            log.warn("Outbox 事件发送失败 eventId={}, 将按重试间隔重发: {}",
                    event.getEventId(), e.getMessage());
            outboxMapper.markRetry(event.getId(), leaseOwner, now.plus(retryInterval),
                    truncate(e.getMessage()));
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
