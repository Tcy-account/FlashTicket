package com.flashticket.purchase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.messaging.dto.PurchaseSubmittedEvent;
import com.flashticket.messaging.entity.OutboxEvent;
import com.flashticket.messaging.mapper.OutboxEventMapper;
import com.flashticket.purchase.mapper.PurchaseRequestMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 抢票请求恢复服务（M12 核心，无 MQ 依赖，集成测试可直接调用）。
 *
 * <p>处理两类「结果不确定」请求的受控恢复，由 {@link PurchaseRecoveryScanner}
 * 周期性驱动（test profile 下关闭调度）：
 * <ol>
 *   <li><b>租约过期</b>：消费者在 PROCESSING 期间崩溃，租约到期仍卡在处理中；
 *       复位为 RECOVERING 并补发新事件。</li>
 *   <li><b>可恢复重试</b>：因技术故障进入 RECOVERING 且到重试时间的请求；领取
 *       恢复权（防多实例重复补发）后补发新事件，交由消费端重新领取租约处理。</li>
 *   <li><b>排队超时</b>：QUEUED 且超过受理截止时间仍未处理的请求，落 FAILED
 *       终态（原因 QUEUE_TIMEOUT），不再等待。</li>
 * </ol>
 *
 * <p>补发复用原 requestId、生成新 eventId：不绕过原限购与预占记录，重复投递由
 * 请求终态 + 订单唯一索引 + Inbox 三重兜底，绝不产生重复订单。
 */
@Service
public class PurchaseRecoveryService {

    private final PurchaseRequestMapper requestMapper;
    private final OutboxEventMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final String purchaseTopic;
    private final int recoveryIntervalSeconds;

    public PurchaseRecoveryService(PurchaseRequestMapper requestMapper,
                                   OutboxEventMapper outboxMapper,
                                   ObjectMapper objectMapper,
                                   @Value("${rocketmq.push-consumer.topic:ft-purchase}") String purchaseTopic,
                                   @Value("${flashticket.purchase.recovery-interval-seconds:30}") int recoveryIntervalSeconds) {
        this.requestMapper = requestMapper;
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.purchaseTopic = purchaseTopic;
        this.recoveryIntervalSeconds = recoveryIntervalSeconds;
    }

    /**
     * 复位租约过期的处理中请求并补发事件。
     *
     * @return true 表示本次成功复位并补发（应由调度器视为已处理）
     */
    @Transactional
    public boolean recoverLeaseExpired(String requestId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (requestMapper.resetLeaseExpired(requestId, now) == 0) {
            return false; // 已被其他实例复位或状态已变化，跳过
        }
        insertOutbox(requestId, now);
        return true;
    }

    /**
     * 领取恢复权并补发可恢复请求。
     *
     * @return true 表示本次成功领取恢复权并补发
     */
    @Transactional
    public boolean recover(String requestId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextRetryAt = now.plusSeconds(recoveryIntervalSeconds);
        if (requestMapper.claimRecovery(requestId, now, nextRetryAt) == 0) {
            return false; // 其他实例已领取恢复权或未到重试时间
        }
        insertOutbox(requestId, now);
        return true;
    }

    /**
     * 排队超时请求落 FAILED 终态（QUEUE_TIMEOUT）。
     */
    @Transactional
    public void failQueueTimeout(String requestId) {
        requestMapper.markQueueTimeout(requestId, LocalDateTime.now(ZoneOffset.UTC));
    }

    /* ---------- 内部 ---------- */

    /** 为请求补发一个全新的 Outbox 事件（新 eventId、原 requestId）。 */
    private void insertOutbox(String requestId, LocalDateTime now) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(eventId);
        outbox.setAggregateType("PURCHASE_REQUEST");
        outbox.setAggregateId(requestId);
        outbox.setTopic(purchaseTopic);
        outbox.setEventType("PURCHASE_SUBMITTED");
        outbox.setPayload(writePayload(eventId, requestId));
        outbox.setSchemaVersion(1);
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outboxMapper.insert(outbox);
    }

    private String writePayload(String eventId, String requestId) {
        try {
            return objectMapper.writeValueAsString(PurchaseSubmittedEvent.of(eventId, requestId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化抢票事件失败", e);
        }
    }
}
