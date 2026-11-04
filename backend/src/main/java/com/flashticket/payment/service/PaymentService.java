package com.flashticket.payment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.coupon.service.UserCouponService;
import com.flashticket.entitlement.service.EntitlementService;
import com.flashticket.inventory.service.InventoryService;
import com.flashticket.notification.enums.NotificationEventType;
import com.flashticket.notification.service.NotificationEventPublisher;
import com.flashticket.order.entity.Order;
import com.flashticket.order.enums.OrderOperationType;
import com.flashticket.order.enums.OrderStatus;
import com.flashticket.order.service.OrderService;
import com.flashticket.payment.dto.PaymentResponse;
import com.flashticket.payment.entity.Payment;
import com.flashticket.payment.entity.PaymentAttempt;
import com.flashticket.payment.entity.PaymentCallback;
import com.flashticket.payment.enums.PaymentAttemptStatus;
import com.flashticket.payment.enums.PaymentStatus;
import com.flashticket.payment.mapper.PaymentAttemptMapper;
import com.flashticket.payment.mapper.PaymentCallbackMapper;
import com.flashticket.payment.mapper.PaymentMapper;
import com.flashticket.risk.service.RateLimitService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟支付编排服务（M10）：支付单创建、模拟执行与结果确认。
 *
 * <p>M10 采用内部 MOCK_WALLET 渠道，同步执行、不接 RocketMQ（见 doc/模拟支付与退款模块.md 二）。
 * 核心是 {@link #mockPay}：在同一 MySQL 本地事务内完成
 * <ol>
 *   <li>条件更新订单 PENDING_PAYMENT → PAID（与超时关单争夺状态转换权，唯一合法终态）；</li>
 *   <li>库存确认 reserved → sold（{@link InventoryService#confirm} 幂等）；</li>
 *   <li>生成票务权益（购买数量 → 对应数量有效门票）；</li>
 *   <li>支付单/尝试置 SUCCEEDED + 写订单操作流水 PAYMENT_CONFIRMED。</li>
 * </ol>
 *
 * <p>支付与关单竞争边界：{@link OrderService#markPaid} 与关单的 closeById 都是
 * {@code WHERE status='PENDING_PAYMENT'} 条件更新，MySQL 行锁 + 当前读语义保证
 * 二者只能有一个成功，不会出现 PAID 与 CLOSED 同时成立（见 doc/模拟支付与退款模块.md 五）。
 */
@Service
public class PaymentService {

    private static final String CHANNEL = "MOCK_WALLET";
    private static final String CURRENCY = "CNY";

    private final PaymentMapper paymentMapper;
    private final PaymentAttemptMapper attemptMapper;
    private final PaymentCallbackMapper callbackMapper;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final EntitlementService entitlementService;
    private final UserCouponService userCouponService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final RateLimitService rateLimitService;

    public PaymentService(PaymentMapper paymentMapper,
                          PaymentAttemptMapper attemptMapper,
                          PaymentCallbackMapper callbackMapper,
                          OrderService orderService,
                          InventoryService inventoryService,
                          EntitlementService entitlementService,
                          UserCouponService userCouponService,
                          NotificationEventPublisher notificationEventPublisher,
                          RateLimitService rateLimitService) {
        this.paymentMapper = paymentMapper;
        this.attemptMapper = attemptMapper;
        this.callbackMapper = callbackMapper;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.entitlementService = entitlementService;
        this.userCouponService = userCouponService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.rateLimitService = rateLimitService;
    }

    /**
     * 创建或获取待支付订单的支付业务单（一订单一支付单）。
     */
    @Transactional
    public PaymentResponse createOrGet(Long orderId, Long userId) {
        Payment existing = paymentMapper.findByOrderId(orderId);
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.PAYMENT_NOT_OWNED);
            }
            return toResponse(existing);
        }

        Order order = orderService.getOrderEntity(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.PAYMENT_ORDER_NOT_PAYABLE);
        }
        if (order.getExpireAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BizException(ErrorCode.PAYMENT_ORDER_EXPIRED);
        }

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setChannel(CHANNEL);
        payment.setAmountCent(order.getPayableAmountCent());
        payment.setCurrency(CURRENCY);
        payment.setStatus(PaymentStatus.CREATED.name());
        payment.setExpireAt(order.getExpireAt());
        payment.setVersion(0L);
        try {
            paymentMapper.insert(payment);
        } catch (DuplicateKeyException e) {
            // 并发创建：uk_payment_order 兜底，重查返回既有支付单
            return toResponse(paymentMapper.findByOrderId(orderId));
        }
        return toResponse(payment);
    }

    /**
     * 查询支付单状态（归属校验）。
     */
    public PaymentResponse getMine(Long userId, Long paymentId) {
        Payment payment = requirePayment(paymentId);
        if (!payment.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PAYMENT_NOT_OWNED);
        }
        return toResponse(payment);
    }

    /**
     * 模拟执行支付：创建尝试 → 模拟渠道确认成功 → 支付确认（订单/库存/权益同事务）。
     *
     * @param attemptRequestId 客户端幂等请求号（重复提交只返回原结果，不重复确认）
     */
    @Transactional
    public PaymentResponse mockPay(Long paymentId, Long userId, String attemptRequestId) {
        Payment payment = requirePayment(paymentId);
        if (!payment.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PAYMENT_NOT_OWNED);
        }

        // 幂等：已成功直接返回
        if (PaymentStatus.SUCCEEDED.name().equals(payment.getStatus())) {
            return toResponse(payment);
        }
        // 终态：已关闭/失败不可再支付
        if (PaymentStatus.CLOSED.name().equals(payment.getStatus())
                || PaymentStatus.FAILED.name().equals(payment.getStatus())) {
            throw new BizException(ErrorCode.PAYMENT_ORDER_CLOSED);
        }

        // 订单资格校验：必须待支付且未过期（已到期的订单即使关单任务未扫到也不可支付）
        Order order = orderService.getOrderEntity(payment.getOrderId());
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.PAYMENT_ORDER_NOT_PAYABLE);
        }
        if (order.getExpireAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BizException(ErrorCode.PAYMENT_ORDER_EXPIRED);
        }

        // 创建支付尝试（attempt_request_id 幂等，重复提交返回 null）
        PaymentAttempt attempt = createAttempt(payment, attemptRequestId);
        if (attempt == null) {
            return toResponse(payment);
        }

        // 限流：同订单窗口内最多 N 次新尝试（M15-A，幂等重试在 createAttempt 已短路）
        rateLimitService.checkPay(payment.getOrderId());

        // 模拟渠道产生结果通知（MOCK_WALLET 确定性成功），写回调（渠道事件唯一去重）
        writeCallback(payment, attempt);

        // 核心确认：订单 PAID + 库存 confirm + 权益生成 + 支付单/尝试 SUCCEEDED + 流水
        confirm(payment, order, attempt);

        return toResponse(requirePayment(payment.getId()));
    }

    /** 管理端支付分页（payment:read）。 */
    public PageResult<PaymentResponse> listAdmin(String status, String paymentNo,
                                                  long page, long pageSize) {
        Page<Payment> p = new Page<>(page, pageSize);
        IPage<Payment> result = paymentMapper.selectAdminPage(p,
                trimToNull(status), trimToNull(paymentNo));
        return new PageResult<>(result.getRecords().stream().map(PaymentService::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 管理端支付详情（payment:read）。 */
    public PaymentResponse getAdmin(Long paymentId) {
        return toResponse(requirePayment(paymentId));
    }

    /* ---------- 内部 ---------- */

    /**
     * 支付确认核心（在 mockPay 的同一本地事务内执行）。
     */
    private void confirm(Payment payment, Order order, PaymentAttempt attempt) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 1. 抢订单状态转换权（与关单互斥，唯一合法终态）
        boolean won = orderService.markPaid(order.getId(), payment.getAmountCent(), now);
        if (!won) {
            // 未抢到：订单已被关单（或被并发支付，V1 一订单一支付单不会发生）
            throw new BizException(ErrorCode.PAYMENT_ORDER_CLOSED);
        }

        // 2. 库存确认 reserved → sold（confirm 按 requestId 幂等）
        inventoryService.confirm(order.getRequestId());

        // 3. 核销优惠券（若使用了券：LOCKED → USED，与订单 PAID 同事务）
        if (order.getUserCouponId() != null) {
            userCouponService.use(order.getUserCouponId(), order.getId(), now);
        }

        // 4. 生成票务权益（购买数量 → 对应数量有效门票）
        entitlementService.issue(order.getId(), order.getUserId(),
                orderService.getOrderItems(order.getId()));

        // 5. 支付单/尝试置 SUCCEEDED
        paymentMapper.markSucceeded(payment.getId(), attempt.getId(), now);
        attemptMapper.markSucceeded(attempt.getId(), now);

        // 6. 订单操作流水 PAYMENT_CONFIRMED
        orderService.appendOperation(order.getId(), OrderOperationType.PAYMENT_CONFIRMED,
                OrderStatus.PENDING_PAYMENT.name(), OrderStatus.PAID.name(),
                payment.getUserId(), null);

        // 7. 支付成功通知（与支付终态同事务写 Outbox，异步消费生成站内消息）
        notificationEventPublisher.publish(NotificationEventType.PAYMENT_SUCCESS,
                order.getUserId(), String.valueOf(order.getId()));
    }

    /**
     * 创建支付尝试（attempt_no 自增，(payment_id, attempt_no)/attempt_request_id 唯一兜底）。
     *
     * @return 新建尝试；若 attempt_request_id 已存在（幂等）返回 null
     */
    private PaymentAttempt createAttempt(Payment payment, String attemptRequestId) {
        PaymentAttempt existing = attemptMapper.findByAttemptRequestId(attemptRequestId);
        if (existing != null) {
            return null;
        }
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentId(payment.getId());
        attempt.setAttemptNo(attemptMapper.countByPaymentId(payment.getId()) + 1);
        attempt.setAttemptRequestId(attemptRequestId);
        attempt.setChannelRequestNo("CH_" + UUID.randomUUID());
        attempt.setStatus(PaymentAttemptStatus.PROCESSING.name());
        attempt.setAmountCent(payment.getAmountCent());
        attempt.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            attemptMapper.insert(attempt);
        } catch (DuplicateKeyException e) {
            return null; // 并发同 attempt_request_id / (payment_id, attempt_no)，幂等
        }
        return attempt;
    }

    /** 写模拟回调（(channel, channel_event_id) 唯一去重，重复通知不重复处理）。 */
    private void writeCallback(Payment payment, PaymentAttempt attempt) {
        String channelEventId = "MOCK_" + UUID.randomUUID();
        PaymentCallback callback = new PaymentCallback();
        callback.setPaymentId(payment.getId());
        callback.setAttemptId(attempt.getId());
        callback.setChannel(CHANNEL);
        callback.setChannelEventId(channelEventId);
        callback.setStatus("PROCESSED");
        callback.setResultCode("SUCCESS");
        callback.setReceivedAt(LocalDateTime.now(ZoneOffset.UTC));
        callbackMapper.insert(callback);
    }

    private Payment requirePayment(Long paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return payment;
    }

    /** 支付业务编号：PAY + 毫秒时间戳 + 4 位随机数（唯一约束兜底并发冲突）。 */
    private String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                String.valueOf(p.getId()), p.getPaymentNo(),
                String.valueOf(p.getOrderId()), String.valueOf(p.getUserId()),
                p.getChannel(), p.getAmountCent(), p.getCurrency(), p.getStatus(),
                p.getExpireAt(), p.getSucceededAt(), p.getCreatedAt());
    }
}
