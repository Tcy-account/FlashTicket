package com.flashticket.refund.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.entitlement.service.EntitlementService;
import com.flashticket.notification.enums.NotificationEventType;
import com.flashticket.notification.service.NotificationEventPublisher;
import com.flashticket.order.entity.Order;
import com.flashticket.order.enums.OrderOperationType;
import com.flashticket.order.enums.OrderStatus;
import com.flashticket.order.service.OrderService;
import com.flashticket.payment.entity.Payment;
import com.flashticket.payment.enums.PaymentStatus;
import com.flashticket.payment.mapper.PaymentMapper;
import com.flashticket.refund.dto.RefundRequest;
import com.flashticket.refund.dto.RefundResponse;
import com.flashticket.refund.entity.Refund;
import com.flashticket.refund.entity.RefundAttempt;
import com.flashticket.refund.enums.RefundAttemptStatus;
import com.flashticket.refund.enums.RefundStatus;
import com.flashticket.refund.mapper.RefundAttemptMapper;
import com.flashticket.refund.mapper.RefundMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款编排服务（M11）：退款申请、财务审核、模拟退款执行与权益撤销。
 *
 * <p>退款是一笔新的逆向交易，不删除原支付记录、不把订单改回待支付（见
 * doc/模拟支付与退款模块.md 六、七）。V1 仅全额退款（金额 = 原支付金额），
 * 同一订单最多一笔退款业务（UNIQUE(order_id)）。退款成功后：
 * <ul>
 *   <li>订单保持 PAID；库存 sold 不变；优惠券保持 USED；</li>
 *   <li>仅撤销票务权益（REVOKED），不自动返还库存与优惠券。</li>
 * </ul>
 *
 * <p>V1 采用内部 MOCK_WALLET 同步模拟退款（不接 RocketMQ），财务审核通过后在同一
 * 本地事务内自动执行退款到 SUCCEEDED（审核字段 approved_by/approved_at 持久化，
 * 执行过程由 ft_refund_attempt 记录）。模块边界：refund(编排) → order/entitlement/
 * payment(只读)，无反向依赖。
 */
@Service
public class RefundService {

    private static final String CHANNEL = "MOCK_WALLET";

    private final RefundMapper refundMapper;
    private final RefundAttemptMapper attemptMapper;
    private final OrderService orderService;
    private final PaymentMapper paymentMapper;
    private final EntitlementService entitlementService;
    private final NotificationEventPublisher notificationEventPublisher;

    public RefundService(RefundMapper refundMapper,
                         RefundAttemptMapper attemptMapper,
                         OrderService orderService,
                         PaymentMapper paymentMapper,
                         EntitlementService entitlementService,
                         NotificationEventPublisher notificationEventPublisher) {
        this.refundMapper = refundMapper;
        this.attemptMapper = attemptMapper;
        this.orderService = orderService;
        this.paymentMapper = paymentMapper;
        this.entitlementService = entitlementService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    /**
     * 用户申请退款（全额，PENDING_REVIEW）。
     *
     * <p>资格：订单属于申请用户且已支付（PAID）、原支付单 SUCCEEDED。V1 同一订单
     * 最多一笔退款业务，重复申请幂等返回既有退款单（UNIQUE(order_id) 兜底）。
     */
    @Transactional
    public RefundResponse apply(Long userId, Long orderId, RefundRequest req) {
        Refund existing = refundMapper.findByOrderId(orderId);
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.REFUND_NOT_OWNED);
            }
            return toResponse(existing); // 重复申请幂等返回
        }

        Order order = orderService.getOrderEntity(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED);
        }
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.REFUND_ORDER_NOT_REFUNDABLE);
        }
        Payment payment = paymentMapper.findByOrderId(orderId);
        if (payment == null || !PaymentStatus.SUCCEEDED.name().equals(payment.getStatus())) {
            throw new BizException(ErrorCode.REFUND_ORDER_NOT_REFUNDABLE);
        }

        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setRefundRequestId(req.refundRequestId());
        refund.setPaymentId(payment.getId());
        refund.setOrderId(orderId);
        refund.setUserId(userId);
        refund.setAmountCent(payment.getAmountCent()); // 全额退款 = 原支付金额
        refund.setCurrency(payment.getCurrency());
        refund.setReasonCode(req.reasonCode());
        refund.setReasonDetail(trimToNull(req.reasonDetail()));
        refund.setStatus(RefundStatus.PENDING_REVIEW.name());
        refund.setVersion(0L);
        try {
            refundMapper.insert(refund);
        } catch (DuplicateKeyException e) {
            // uk_refund_order / uk_refund_request / uk_refund_no 兜底，幂等返回
            return toResponse(refundMapper.findByOrderId(orderId));
        }

        // 订单操作流水：申请退款（订单主状态保持 PAID）
        orderService.appendOperation(orderId, OrderOperationType.REFUND_REQUESTED,
                OrderStatus.PAID.name(), OrderStatus.PAID.name(), userId, refund.getReasonCode());

        // 退款申请受理通知（与申请同事务写 Outbox）
        notificationEventPublisher.publish(NotificationEventType.REFUND_REQUESTED,
                userId, String.valueOf(refund.getId()));
        return toResponse(refund);
    }

    /**
     * 用户撤销未审核的退款申请（PENDING_REVIEW → CANCELLED，幂等）。
     */
    @Transactional
    public RefundResponse cancel(Long userId, Long refundId) {
        Refund refund = requireRefund(refundId);
        if (!refund.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.REFUND_NOT_OWNED);
        }
        if (RefundStatus.PENDING_REVIEW.name().equals(refund.getStatus())) {
            refundMapper.markCancelled(refundId, LocalDateTime.now(ZoneOffset.UTC));
            // 重查最新状态：成功则 CANCELLED，并发被审核/撤销则返回终态（幂等）
            refund = requireRefund(refundId);
        }
        return toResponse(refund);
    }

    /** 查询退款进度（归属校验）。 */
    public RefundResponse getMine(Long userId, Long refundId) {
        Refund refund = requireRefund(refundId);
        if (!refund.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.REFUND_NOT_OWNED);
        }
        return toResponse(refund);
    }

    /** 我的退款分页（按创建时间倒序，可按状态筛选）。 */
    public PageResult<RefundResponse> listMine(Long userId, String status, long page, long pageSize) {
        Page<Refund> p = new Page<>(page, pageSize);
        IPage<Refund> result = refundMapper.selectUserPage(p, userId, trimToNull(status));
        return new PageResult<>(result.getRecords().stream().map(RefundService::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 财务审核通过（refund:approve），并自动执行模拟退款到 SUCCEEDED。
     *
     * <p>审核人不能审核自己提交的申请（REFUND_SELF_REVIEW）。审核通过后在同一本地
     * 事务内：创建执行尝试 → 撤销权益 → 退款单 APPROVED → SUCCEEDED。重复审核幂等。
     */
    @Transactional
    public RefundResponse approve(Long reviewerId, Long refundId, String comment) {
        Refund refund = requireRefund(refundId);
        if (refund.getUserId().equals(reviewerId)) {
            throw new BizException(ErrorCode.REFUND_SELF_REVIEW);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = refundMapper.markApproved(refundId, reviewerId, now, trimToNull(comment));
        if (updated == 0) {
            Refund latest = requireRefund(refundId);
            if (RefundStatus.SUCCEEDED.name().equals(latest.getStatus())
                    || RefundStatus.APPROVED.name().equals(latest.getStatus())) {
                return toResponse(latest); // 并发/重复审核幂等
            }
            throw new BizException(ErrorCode.REFUND_ILLEGAL_TRANSITION);
        }

        // 审核通过后自动执行模拟退款（同事务）
        executeRefund(refund, reviewerId);
        return toResponse(requireRefund(refundId));
    }

    /**
     * 财务审核拒绝（refund:approve，PENDING_REVIEW → REJECTED）。
     *
     * <p>审核拒绝必须填写原因；审核人不能审核自己提交的申请。
     */
    @Transactional
    public RefundResponse reject(Long reviewerId, Long refundId, String comment) {
        Refund refund = requireRefund(refundId);
        if (refund.getUserId().equals(reviewerId)) {
            throw new BizException(ErrorCode.REFUND_SELF_REVIEW);
        }
        if (comment == null || comment.isBlank()) {
            throw new BizException(ErrorCode.REFUND_REVIEW_COMMENT_REQUIRED);
        }

        int updated = refundMapper.markRejected(refundId, reviewerId,
                LocalDateTime.now(ZoneOffset.UTC), comment.trim());
        if (updated == 0) {
            Refund latest = requireRefund(refundId);
            if (RefundStatus.REJECTED.name().equals(latest.getStatus())) {
                return toResponse(latest); // 重复拒绝幂等
            }
            throw new BizException(ErrorCode.REFUND_ILLEGAL_TRANSITION);
        }

        // 退款拒绝通知（与审核同事务写 Outbox）
        notificationEventPublisher.publish(NotificationEventType.REFUND_REJECTED,
                refund.getUserId(), String.valueOf(refund.getId()));
        return toResponse(requireRefund(refundId));
    }

    /**
     * 财务重试退款执行（refund:retry）：对 FAILED 的退款单复用业务单重新执行。
     */
    @Transactional
    public RefundResponse retry(Long adminId, Long refundId) {
        Refund refund = requireRefund(refundId);
        if (RefundStatus.SUCCEEDED.name().equals(refund.getStatus())) {
            return toResponse(refund); // 已成功幂等
        }
        if (!RefundStatus.FAILED.name().equals(refund.getStatus())) {
            throw new BizException(ErrorCode.REFUND_ILLEGAL_TRANSITION);
        }
        executeRefund(refund, adminId);
        return toResponse(requireRefund(refundId));
    }

    /** 管理端退款分页（refund:read）。 */
    public PageResult<RefundResponse> listAdmin(String status, String refundNo,
                                                long page, long pageSize) {
        Page<Refund> p = new Page<>(page, pageSize);
        IPage<Refund> result = refundMapper.selectAdminPage(p,
                trimToNull(status), trimToNull(refundNo));
        return new PageResult<>(result.getRecords().stream().map(RefundService::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 管理端退款详情（refund:read）。 */
    public RefundResponse getAdmin(Long refundId) {
        return toResponse(requireRefund(refundId));
    }

    /* ---------- 内部 ---------- */

    /**
     * 模拟退款执行核心（在 approve/retry 的同一本地事务内执行）：
     * 创建执行尝试 → 撤销票务权益 → 退款单 SUCCEEDED → 尝试 SUCCEEDED → 写流水。
     */
    private void executeRefund(Refund refund, Long actorId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 1. 创建退款执行尝试（PROCESSING，(refund_id, attempt_no)/channel_request_no 唯一）
        RefundAttempt attempt = createAttempt(refund, now);

        // 2. 撤销票务权益（VALID → REVOKED，幂等）
        entitlementService.revokeByOrderId(refund.getOrderId());

        // 3. 退款单 APPROVED/FAILED → SUCCEEDED（条件更新，与并发确认互斥）
        int won = refundMapper.markSucceeded(refund.getId(), attempt.getId(), now);
        if (won == 0) {
            // 已被并发确认成功，幂等
            return;
        }

        // 4. 尝试 SUCCEEDED
        attemptMapper.markSucceeded(attempt.getId(), now);

        // 5. 订单操作流水：退款成功（订单主状态保持 PAID）
        orderService.appendOperation(refund.getOrderId(), OrderOperationType.REFUND_SUCCEEDED,
                OrderStatus.PAID.name(), OrderStatus.PAID.name(), actorId, null);

        // 退款成功通知（与退款终态同事务写 Outbox）
        notificationEventPublisher.publish(NotificationEventType.REFUND_SUCCESS,
                refund.getUserId(), String.valueOf(refund.getId()));
    }

    /** 创建退款执行尝试（PROCESSING，attempt_no 递增，(refund_id, attempt_no) 唯一兜底）。 */
    private RefundAttempt createAttempt(Refund refund, LocalDateTime now) {
        int attemptNo = attemptMapper.countByRefundId(refund.getId()) + 1;
        RefundAttempt attempt = new RefundAttempt();
        attempt.setRefundId(refund.getId());
        attempt.setAttemptNo(attemptNo);
        attempt.setChannelRequestNo("RF_" + UUID.randomUUID());
        attempt.setStatus(RefundAttemptStatus.PROCESSING.name());
        attempt.setAmountCent(refund.getAmountCent());
        attempt.setStartedAt(now);
        try {
            attemptMapper.insert(attempt);
        } catch (DuplicateKeyException e) {
            // (refund_id, attempt_no) / channel_request_no 冲突：重查返回既有尝试（幂等）
            RefundAttempt existing = attemptMapper.findByRefundIdAndAttemptNo(refund.getId(), attemptNo);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
        return attempt;
    }

    private Refund requireRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BizException(ErrorCode.REFUND_NOT_FOUND);
        }
        return refund;
    }

    /** 退款业务号：RF + 毫秒时间戳 + 4 位随机数（唯一约束兜底并发冲突）。 */
    private String generateRefundNo() {
        return "RF" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static RefundResponse toResponse(Refund r) {
        return new RefundResponse(
                String.valueOf(r.getId()), r.getRefundNo(), r.getRefundRequestId(),
                String.valueOf(r.getPaymentId()), String.valueOf(r.getOrderId()),
                String.valueOf(r.getUserId()), r.getAmountCent(), r.getCurrency(),
                r.getReasonCode(), r.getReasonDetail(), r.getStatus(),
                r.getApprovedBy() == null ? null : String.valueOf(r.getApprovedBy()),
                r.getApprovedAt(),
                r.getRejectedBy() == null ? null : String.valueOf(r.getRejectedBy()),
                r.getRejectedAt(), r.getReviewComment(),
                r.getSucceededAt(), r.getFailedAt(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
