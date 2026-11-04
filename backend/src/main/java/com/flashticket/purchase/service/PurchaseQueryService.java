package com.flashticket.purchase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.messaging.dto.PurchaseSubmittedEvent;
import com.flashticket.messaging.entity.OutboxEvent;
import com.flashticket.messaging.mapper.OutboxEventMapper;
import com.flashticket.order.dto.OrderResponse;
import com.flashticket.order.service.OrderService;
import com.flashticket.purchase.dto.PurchaseStatusResponse;
import com.flashticket.purchase.entity.PurchaseRequest;
import com.flashticket.purchase.enums.PurchaseStatus;
import com.flashticket.purchase.mapper.PurchaseRequestMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 抢票请求查询/取消/管理端重试服务（M12）。
 *
 * <p>抢票状态以 ft_purchase_request 为权威来源（前端排队页轮询）。SUCCEEDED 时
 * 附带订单详情；FAILED 时附失败原因。取消仅允许 QUEUED（未取得执行权的请求），
 * 已建单应走订单取消接口。
 */
@Service
public class PurchaseQueryService {

    private final PurchaseRequestMapper requestMapper;
    private final OrderService orderService;
    private final OutboxEventMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final String purchaseTopic;

    public PurchaseQueryService(PurchaseRequestMapper requestMapper,
                                OrderService orderService,
                                OutboxEventMapper outboxMapper,
                                ObjectMapper objectMapper,
                                @Value("${rocketmq.push-consumer.topic:ft-purchase}") String purchaseTopic) {
        this.requestMapper = requestMapper;
        this.orderService = orderService;
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.purchaseTopic = purchaseTopic;
    }

    /** 查询抢票状态（归属校验）。 */
    public PurchaseStatusResponse getStatus(Long userId, String requestId) {
        return toStatusResponse(requireOwned(userId, requestId));
    }

    /** 我的抢票历史分页。 */
    public PageResult<PurchaseStatusResponse> listMy(Long userId, long page, long pageSize) {
        Page<PurchaseRequest> p = new Page<>(page, pageSize);
        IPage<PurchaseRequest> result = requestMapper.selectUserPage(p, userId);
        return new PageResult<>(result.getRecords().stream().map(this::toStatusResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 撤销尚未开始处理的排队请求（仅 QUEUED 可撤销，幂等）。 */
    public PurchaseStatusResponse cancel(Long userId, String requestId) {
        PurchaseRequest pr = requireOwned(userId, requestId);
        if (PurchaseStatus.QUEUED.name().equals(pr.getStatus())) {
            requestMapper.markCancelled(requestId, LocalDateTime.now(ZoneOffset.UTC));
            // 条件更新后重查权威状态：撤销成功 → CANCELLED；并发已被消费者领取 → 返回最新态。
            pr = requestMapper.findByRequestId(requestId);
        } else if (!PurchaseStatus.CANCELLED.name().equals(pr.getStatus())) {
            throw new BizException(ErrorCode.PURCHASE_NOT_CANCELLABLE);
        }
        return toStatusResponse(pr);
    }

    /** 管理端抢票请求分页（purchase:read）。 */
    public PageResult<PurchaseStatusResponse> listAdmin(String status, long page, long pageSize) {
        Page<PurchaseRequest> p = new Page<>(page, pageSize);
        IPage<PurchaseRequest> result = requestMapper.selectAdminPage(p, trimToNull(status));
        return new PageResult<>(result.getRecords().stream().map(this::toStatusResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 管理端抢票请求详情（purchase:read）。 */
    public PurchaseStatusResponse getAdmin(String requestId) {
        return toStatusResponse(require(requestId));
    }

    /**
     * 管理端重试可恢复请求（purchase:retry）：为 RECOVERING 请求补发 Outbox 事件
     * （新 eventId），并复位重试时间允许立即重新领取租约。复用原 requestId，
     * 不绕过原限购与预占记录（见 doc/抢票请求与异步排队模块.md 十二）。
     */
    @Transactional
    public PurchaseStatusResponse retry(String requestId) {
        PurchaseRequest pr = require(requestId);
        if (!PurchaseStatus.RECOVERING.name().equals(pr.getStatus())) {
            throw new BizException(ErrorCode.PURCHASE_NOT_CANCELLABLE);
        }

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

        requestMapper.markRetryable(requestId);
        return toStatusResponse(requestMapper.findByRequestId(requestId));
    }

    /* ---------- 内部 ---------- */

    private PurchaseRequest require(String requestId) {
        PurchaseRequest pr = requestMapper.findByRequestId(requestId);
        if (pr == null) {
            throw new BizException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        return pr;
    }

    private PurchaseRequest requireOwned(Long userId, String requestId) {
        PurchaseRequest pr = require(requestId);
        if (!pr.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED);
        }
        return pr;
    }

    private PurchaseStatusResponse toStatusResponse(PurchaseRequest pr) {
        OrderResponse order = null;
        if (PurchaseStatus.SUCCEEDED.name().equals(pr.getStatus())) {
            order = orderService.getByRequestId(pr.getRequestId());
        }
        return new PurchaseStatusResponse(
                pr.getRequestId(), pr.getStatus(), pr.getFailReason(),
                pr.getOrderId() == null ? null : String.valueOf(pr.getOrderId()),
                order, pr.getAttemptCount(), pr.getCreatedAt(), pr.getCompletedAt());
    }

    private String writePayload(String eventId, String requestId) {
        try {
            return objectMapper.writeValueAsString(PurchaseSubmittedEvent.of(eventId, requestId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化抢票事件失败", e);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
