package com.flashticket.purchase.service;

import com.flashticket.common.exception.BizException;
import com.flashticket.messaging.entity.MessageInbox;
import com.flashticket.messaging.mapper.MessageInboxMapper;
import com.flashticket.notification.enums.NotificationEventType;
import com.flashticket.notification.service.NotificationEventPublisher;
import com.flashticket.order.dto.OrderSnapshot;
import com.flashticket.order.service.OrderCloseDelaySender;
import com.flashticket.purchase.entity.PurchaseRequest;
import com.flashticket.purchase.enums.PurchaseStatus;
import com.flashticket.purchase.mapper.PurchaseRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 抢票消费处理编排（M12 核心，无 MQ 依赖，集成测试可直接调用）。
 *
 * <p>处理一条抢票事件：Inbox 幂等 → 查询权威请求状态 → 领取处理租约 →
 * 复检资格 → 建单事务（成功/业务失败/技术故障三分支）。RocketMQ 的至少一次
 * 投递由 request_id 唯一、租约条件更新与 Inbox 三重兜底，保证不重复建单。
 *
 * <p>消费返回语义（listener 据此决定 MQ 重试）：
 * <ul>
 *   <li>业务确定结果（成功/业务失败/幂等跳过）→ 消费完成，不重试；</li>
 *   <li>技术故障 → 标记 RECOVERING，由 MQ 重试或恢复扫描器补发兜底。</li>
 * </ul>
 */
@Service
public class PurchaseRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRequestProcessor.class);

    /** 消费组名，同时作为 Inbox 的 consumer_name 去重维度。 */
    public static final String CONSUMER_NAME = "ft-purchase-cg";

    private final PurchaseRequestMapper requestMapper;
    private final MessageInboxMapper inboxMapper;
    private final PurchaseEligibilityService eligibilityService;
    private final PurchaseOrderBuilder orderBuilder;
    private final ObjectProvider<OrderCloseDelaySender> closeDelaySenderProvider;
    private final NotificationEventPublisher notificationEventPublisher;
    private final int leaseSeconds;
    private final int retrySeconds;
    private final int paymentTimeoutMinutes;

    public PurchaseRequestProcessor(PurchaseRequestMapper requestMapper,
                                    MessageInboxMapper inboxMapper,
                                    PurchaseEligibilityService eligibilityService,
                                    PurchaseOrderBuilder orderBuilder,
                                    ObjectProvider<OrderCloseDelaySender> closeDelaySenderProvider,
                                    NotificationEventPublisher notificationEventPublisher,
                                    @Value("${flashticket.purchase.lease-seconds:30}") int leaseSeconds,
                                    @Value("${flashticket.purchase.retry-seconds:5}") int retrySeconds,
                                    @Value("${flashticket.order.payment-timeout-minutes:15}") int paymentTimeoutMinutes) {
        this.requestMapper = requestMapper;
        this.inboxMapper = inboxMapper;
        this.eligibilityService = eligibilityService;
        this.orderBuilder = orderBuilder;
        this.closeDelaySenderProvider = closeDelaySenderProvider;
        this.notificationEventPublisher = notificationEventPublisher;
        this.leaseSeconds = leaseSeconds;
        this.retrySeconds = retrySeconds;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    /**
     * 处理抢票事件。
     *
     * @param eventId   MQ 事件标识（Inbox 去重键）
     * @param requestId 抢票请求号
     * @return true 表示事件已确定处理完成（成功/业务失败/幂等跳过）；false 表示
     *         技术故障、请求进入 RECOVERING，等待重试或恢复。
     */
    public boolean process(String eventId, String requestId) {
        // 1. Inbox 幂等：同一事件已消费，直接跳过
        if (inboxMapper.countByConsumerAndEvent(CONSUMER_NAME, eventId) > 0) {
            return true;
        }

        // 2. 查询权威请求状态
        PurchaseRequest request = requestMapper.findByRequestId(requestId);
        if (request == null) {
            // 受理记录丢失（极端），记录 Inbox 防消息无限重试
            recordInbox(eventId);
            return true;
        }

        // 3. 已终态则幂等返回（SUCCEEDED/FAILED/CANCELLED 不再处理）
        if (isTerminal(request.getStatus())) {
            recordInbox(eventId);
            return true;
        }

        // 4. 领取处理租约：QUEUED/RECOVERING → PROCESSING
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String leaseOwner = UUID.randomUUID().toString();
        LocalDateTime leaseUntil = now.plusSeconds(leaseSeconds);
        if (requestMapper.acquireLease(requestId, leaseOwner, leaseUntil, now) == 0) {
            // 其他消费者处理中或未到重试时间，交由持有者/恢复扫描器推进
            return true;
        }

        // 5. 复检资格 + 建单事务
        try {
            OrderSnapshot snapshot = eligibilityService.validateAndSnapshot(
                    request.getTierId(), request.getQuantity());
            Long orderId = orderBuilder.build(requestId, request.getUserId(), request.getQuantity(),
                    leaseOwner, snapshot, eventId, CONSUMER_NAME, request.getSelectedCouponId());
            // 建单成功：安排支付超时延迟关单消息（发送失败由 DB 兜底扫描器保障）
            scheduleDelayClose(orderId);
            return true;
        } catch (LeaseLostException e) {
            // 租约已被收回，交由新处理者，本消息视作完成
            return true;
        } catch (BizException e) {
            // 确定性业务失败：终态 FAILED + 消费记录
            requestMapper.markFailed(requestId, failReason(e), leaseOwner, now);
            recordInbox(eventId);

            // 抢票失败通知（尽力而为：请求已 FAILED 可查，通知丢失不影响业务）
            notificationEventPublisher.publish(NotificationEventType.PURCHASE_FAILED,
                    request.getUserId(), requestId);
            return true;
        } catch (Exception e) {
            // 技术故障：进入 RECOVERING，等待恢复（MQ 重试或扫描器补发）
            log.warn("抢票处理技术故障 requestId={}: {}", requestId, e.getMessage());
            requestMapper.markRecovering(requestId, "TECHNICAL", now.plusSeconds(retrySeconds), leaseOwner);
            return false;
        }
    }

    /* ---------- 内部 ---------- */

    /** 安排支付超时延迟关单（发送器仅在非 test profile 存在，发送失败静默降级）。 */
    private void scheduleDelayClose(Long orderId) {
        OrderCloseDelaySender sender = closeDelaySenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            sender.scheduleClose(orderId, Duration.ofMinutes(paymentTimeoutMinutes));
        } catch (Exception e) {
            log.warn("安排延迟关单失败 orderId={}: {}", orderId, e.getMessage());
        }
    }

    private static boolean isTerminal(String status) {
        return PurchaseStatus.SUCCEEDED.name().equals(status)
                || PurchaseStatus.FAILED.name().equals(status)
                || PurchaseStatus.CANCELLED.name().equals(status);
    }

    /** 业务失败原因码：简短语义，落在 ft_purchase_request.fail_reason（≤32）。 */
    private static String failReason(BizException e) {
        String code = e.getErrorCode().name();
        return code.length() > 32 ? code.substring(0, 32) : code;
    }

    /** 记录消费幂等（并发重复由唯一约束兜底忽略）。 */
    private void recordInbox(String eventId) {
        MessageInbox inbox = new MessageInbox();
        inbox.setConsumerName(CONSUMER_NAME);
        inbox.setEventId(eventId);
        inbox.setProcessedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            inboxMapper.insert(inbox);
        } catch (DuplicateKeyException e) {
            // 并发已记录，忽略
        }
    }
}
