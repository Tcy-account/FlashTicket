package com.flashticket.purchase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.inventory.enums.AdmissionResult;
import com.flashticket.inventory.service.InventoryAdmissionService;
import com.flashticket.messaging.dto.PurchaseSubmittedEvent;
import com.flashticket.messaging.entity.OutboxEvent;
import com.flashticket.messaging.mapper.OutboxEventMapper;
import com.flashticket.order.dto.OrderResponse;
import com.flashticket.order.dto.OrderSnapshot;
import com.flashticket.order.service.OrderService;
import com.flashticket.purchase.dto.CheckoutRequest;
import com.flashticket.purchase.dto.CheckoutResponse;
import com.flashticket.purchase.entity.PurchaseRequest;
import com.flashticket.purchase.enums.PurchaseStatus;
import com.flashticket.purchase.mapper.PurchaseRequestMapper;
import com.flashticket.risk.service.RateLimitService;
import com.flashticket.risk.service.RiskRestrictionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 抢票受理编排服务（M12）：可靠受理抢票请求并返回排队状态。
 *
 * <p>将 M08 的同步建单升级为「可靠受理」：在同一个 MySQL 本地事务内写入
 * 抢票请求（QUEUED）+ Outbox 事件，返回 HTTP 202。即使应用在响应后崩溃，请求与
 * 待发布事件仍然保留，由 OutboxPublisher 补发到 RocketMQ、消费者异步建单。
 * 真正建单由 {@link PurchaseRequestProcessor}（消费端）完成。
 *
 * <p>受理只做「预检」快速拒绝无效请求，不保证最终抢到（余票由消费端条件扣减裁决）。
 */
@Service
public class PurchaseCheckoutService {

    private final PurchaseRequestMapper requestMapper;
    private final OutboxEventMapper outboxMapper;
    private final PurchaseEligibilityService eligibilityService;
    private final OrderService orderService;
    private final RateLimitService rateLimitService;
    private final RiskRestrictionService riskRestrictionService;
    private final InventoryAdmissionService inventoryAdmissionService;
    private final ObjectMapper objectMapper;
    private final String purchaseTopic;
    private final int acceptDeadlineSeconds;

    public PurchaseCheckoutService(PurchaseRequestMapper requestMapper,
                                   OutboxEventMapper outboxMapper,
                                   PurchaseEligibilityService eligibilityService,
                                   OrderService orderService,
                                   RateLimitService rateLimitService,
                                   RiskRestrictionService riskRestrictionService,
                                   InventoryAdmissionService inventoryAdmissionService,
                                   ObjectMapper objectMapper,
                                   @Value("${rocketmq.push-consumer.topic:ft-purchase}") String purchaseTopic,
                                   @Value("${flashticket.purchase.accept-deadline-seconds:60}") int acceptDeadlineSeconds) {
        this.requestMapper = requestMapper;
        this.outboxMapper = outboxMapper;
        this.eligibilityService = eligibilityService;
        this.orderService = orderService;
        this.rateLimitService = rateLimitService;
        this.riskRestrictionService = riskRestrictionService;
        this.inventoryAdmissionService = inventoryAdmissionService;
        this.objectMapper = objectMapper;
        this.purchaseTopic = purchaseTopic;
        this.acceptDeadlineSeconds = acceptDeadlineSeconds;
    }

    /**
     * 可靠受理抢票请求：返回 QUEUED（或幂等命中返回现有状态）。
     *
     * @param userId 当前登录用户
     */
    @Transactional
    public CheckoutResponse accept(Long userId, CheckoutRequest req) {
        // 1. 幂等：requestId 已存在则返回现有状态（SUCCEEDED 时带订单）
        PurchaseRequest existing = requestMapper.findByRequestId(req.requestId());
        if (existing != null) {
            return toCheckoutResponse(existing);
        }

        Long tierId = Long.valueOf(req.tierId());
        int quantity = req.quantity();

        // 2. 限流：同账号同票档窗口内最多 N 个新请求（M15-A，幂等重试在步骤 1 已短路不消耗）
        rateLimitService.checkPurchase(userId, tierId);

        // 2.5 风控限制检查：被临时限制的用户禁止抢票（M15-B）
        riskRestrictionService.checkRestriction("USER", String.valueOf(userId), "PURCHASE");

        // 3. 资格预检（快速拒绝无效请求，同时确认 eventId/sessionId 供追踪）
        OrderSnapshot snapshot = eligibilityService.validateAndSnapshot(tierId, quantity);

        // 3.5 Redis 高速准入预检（M15-C 模式3）：快照判定不足则入口快速拒绝，
        //     否则受理；快照缺失/Redis 异常回退 MySQL 权威（FALLBACK 不阻断）
        if (inventoryAdmissionService.tryAdmit(tierId, quantity) == AdmissionResult.SOLD_OUT) {
            throw new BizException(ErrorCode.INVENTORY_SOLD_OUT);
        }

        // 4. 写请求 QUEUED + Outbox 事件（同一本地事务，可靠受理）
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String eventId = UUID.randomUUID().toString();

        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestId(req.requestId());
        pr.setUserId(userId);
        pr.setTierId(tierId);
        pr.setQuantity(quantity);
        pr.setSelectedCouponId(parseCouponId(req.selectedCouponId()));
        pr.setEventId(snapshot.eventId());
        pr.setSessionId(snapshot.sessionId());
        pr.setStatus(PurchaseStatus.QUEUED.name());
        pr.setAttemptCount(0);
        pr.setAcceptedDeadlineAt(now.plusSeconds(acceptDeadlineSeconds));
        pr.setVersion(0L);
        requestMapper.insert(pr);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(eventId);
        outbox.setAggregateType("PURCHASE_REQUEST");
        outbox.setAggregateId(req.requestId());
        outbox.setTopic(purchaseTopic);
        outbox.setEventType("PURCHASE_SUBMITTED");
        outbox.setPayload(writePayload(eventId, req.requestId()));
        outbox.setSchemaVersion(1);
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outboxMapper.insert(outbox);

        return new CheckoutResponse(req.requestId(), PurchaseStatus.QUEUED.name(), null);
    }

    /* ---------- 内部 ---------- */

    /** 解析选中的用户优惠券 ID（雪花字符串 → Long，空返回 null）。 */
    private Long parseCouponId(String selectedCouponId) {
        if (selectedCouponId == null || selectedCouponId.isBlank()) {
            return null;
        }
        return Long.valueOf(selectedCouponId);
    }

    private CheckoutResponse toCheckoutResponse(PurchaseRequest pr) {
        OrderResponse order = null;
        if (PurchaseStatus.SUCCEEDED.name().equals(pr.getStatus())) {
            order = orderService.getByRequestId(pr.getRequestId());
        }
        return new CheckoutResponse(pr.getRequestId(), pr.getStatus(), order);
    }

    private String writePayload(String eventId, String requestId) {
        try {
            return objectMapper.writeValueAsString(PurchaseSubmittedEvent.of(eventId, requestId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化抢票事件失败", e);
        }
    }
}
