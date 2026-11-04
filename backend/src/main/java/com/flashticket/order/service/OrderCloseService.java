package com.flashticket.order.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.coupon.service.UserCouponService;
import com.flashticket.inventory.service.InventoryService;
import com.flashticket.notification.enums.NotificationEventType;
import com.flashticket.notification.service.NotificationEventPublisher;
import com.flashticket.order.dto.OrderResponse;
import com.flashticket.order.entity.Order;
import com.flashticket.order.entity.OrderOperation;
import com.flashticket.order.enums.CloseReason;
import com.flashticket.order.enums.OrderOperationType;
import com.flashticket.order.enums.OrderStatus;
import com.flashticket.order.mapper.OrderMapper;
import com.flashticket.order.mapper.OrderOperationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 订单关闭编排服务（M09）：用户取消与支付超时关单。
 *
 * <p>用户取消和系统超时都必须经过同一套关闭逻辑，避免两份代码分别修改库存
 * （见 doc/订单与交易状态机模块.md 六、七）。核心 {@link #doClose} 在同一
 * MySQL 本地事务内完成：
 * <ol>
 *   <li>条件更新订单状态 PENDING_PAYMENT → CLOSED（与支付争夺状态转换权）；</li>
 *   <li>调用库存领域服务 release 释放预占（reserved → available）并归还限购名额；</li>
 *   <li>写订单操作流水（业务审计）。</li>
 * </ol>
 *
 * <p>幂等保证：重复取消/重复调度不会重复释放——条件更新以受影响行数判成败，
 * 未抢到状态转换权的线程重查后按终态幂等返回（见 doc/订单与交易状态机模块.md 十二）。
 */
@Service
public class OrderCloseService {

    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final OrderOperationMapper orderOperationMapper;
    private final OrderService orderService;
    private final UserCouponService userCouponService;
    private final NotificationEventPublisher notificationEventPublisher;

    public OrderCloseService(OrderMapper orderMapper,
                             InventoryService inventoryService,
                             OrderOperationMapper orderOperationMapper,
                             OrderService orderService,
                             UserCouponService userCouponService,
                             NotificationEventPublisher notificationEventPublisher) {
        this.orderMapper = orderMapper;
        this.inventoryService = inventoryService;
        this.orderOperationMapper = orderOperationMapper;
        this.orderService = orderService;
        this.userCouponService = userCouponService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    /**
     * 用户主动取消待支付订单（归属校验）。
     *
     * @return 关闭后的订单详情
     */
    @Transactional
    public OrderResponse cancelByUser(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED);
        }
        doClose(order, CloseReason.USER_CANCEL, userId);
        return orderService.getByRequestId(order.getRequestId());
    }

    /**
     * 支付超时系统关单（供定时扫描调用）。
     *
     * <p>已关闭幂等返回；已被并发支付则抛 {@link ErrorCode#ORDER_NOT_CANCELLABLE}，
     * 由调度器捕获忽略。
     */
    @Transactional
    public void closeByTimeout(Long orderId) {
        Order order = requireOrder(orderId);
        doClose(order, CloseReason.PAYMENT_TIMEOUT, null);
    }

    /* ---------- 内部 ---------- */

    /**
     * 关单核心：条件状态转换 + 库存释放 + 流水，三者同一事务。
     */
    private void doClose(Order order, CloseReason reason, Long actorId) {
        String status = order.getStatus();

        // 已关闭：幂等返回（重复取消/重复调度不重复释放）
        if (OrderStatus.CLOSED.name().equals(status)) {
            return;
        }
        // 已支付（或其他非待支付状态）：不允许关单
        if (!OrderStatus.PENDING_PAYMENT.name().equals(status)) {
            throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }

        // 条件更新：仅 PENDING_PAYMENT → CLOSED（与支付并发竞争状态转换权）
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = orderMapper.closeById(order.getId(), now, reason.name());
        if (updated == 0) {
            // 并发：已被支付或已被关闭，重查判定
            Order latest = orderMapper.selectById(order.getId());
            if (latest != null && OrderStatus.CLOSED.name().equals(latest.getStatus())) {
                return; // 已被关闭，幂等
            }
            throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE); // 已被支付
        }

        // 释放库存预占 + 归还限购名额（release 幂等，重复调用不重复扣）
        inventoryService.release(order.getRequestId());

        // 返券（若使用了券：LOCKED → AVAILABLE/EXPIRED，与订单 CLOSED 同事务）
        if (order.getUserCouponId() != null) {
            userCouponService.release(order.getUserCouponId(), order.getId(), now);
        }

        // 写操作流水（业务审计）
        OrderOperationType type = reason == CloseReason.USER_CANCEL
                ? OrderOperationType.USER_CANCELLED
                : OrderOperationType.TIMEOUT_CLOSED;
        orderOperationMapper.insert(OrderOperation.of(
                order.getId(), type,
                OrderStatus.PENDING_PAYMENT.name(), OrderStatus.CLOSED.name(),
                actorId, reason.name()));

        // 关单通知（与关单终态同事务写 Outbox；用户取消与超时关单均通知）
        notificationEventPublisher.publish(NotificationEventType.ORDER_CLOSED,
                order.getUserId(), String.valueOf(order.getId()));
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }
}
