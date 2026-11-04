package com.flashticket.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.order.dto.OrderItemResponse;
import com.flashticket.order.dto.OrderOperationResponse;
import com.flashticket.order.dto.OrderResponse;
import com.flashticket.order.dto.OrderSnapshot;
import com.flashticket.order.entity.Order;
import com.flashticket.order.entity.OrderItem;
import com.flashticket.order.entity.OrderOperation;
import com.flashticket.order.enums.OrderOperationType;
import com.flashticket.order.enums.OrderStatus;
import com.flashticket.order.mapper.OrderItemMapper;
import com.flashticket.order.mapper.OrderMapper;
import com.flashticket.order.mapper.OrderOperationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单领域服务（M08）：创建订单（交易快照）、我的订单、订单详情与归属校验。
 *
 * <p>规则（见 doc/订单与交易状态机模块.md）：
 * <ul>
 *   <li>订单仅由成功的抢票请求驱动创建，不对用户端开放绕过抢票的普通下单；</li>
 *   <li>order_no / request_id 数据库唯一，防重复订单；</li>
 *   <li>金额与明细为不可变交易快照，不因票价配置变更而更新；</li>
 *   <li>订单支付期限从数据库订单创建时刻起算（M08 固定 15 分钟）。</li>
 * </ul>
 *
 * <p>本服务不直接修改库存；订单与库存预占、限购占用的同事务协调由抢票编排层
 * {@code PurchaseCheckoutService} 负责（模块边界见 doc/跨域审查 6.1）。
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderOperationMapper orderOperationMapper;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                        OrderOperationMapper orderOperationMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderOperationMapper = orderOperationMapper;
    }

    /**
     * 创建待支付订单与明细快照。
     *
     * @param requestId 抢票请求号（order.request_id 唯一）
     * @param userId    买家用户
     * @param snapshot  成交快照
     * @param expireAt  支付截止时间（编排层计算）
     * @return 新建订单
     */
    @Transactional
    public Order createOrder(String requestId, Long userId, OrderSnapshot snapshot, LocalDateTime expireAt,
                             long discountAmountCent, Long userCouponId, Long couponTemplateId,
                             String promotionSnapshot) {
        long amount = snapshot.amountCent();
        long payable = Math.max(0, amount - discountAmountCent);

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setRequestId(requestId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setOriginalAmountCent(amount);
        order.setDiscountAmountCent(discountAmountCent);
        order.setPayableAmountCent(payable);
        order.setUserCouponId(userCouponId);
        order.setCouponTemplateId(couponTemplateId);
        order.setPromotionSnapshot(promotionSnapshot);
        order.setPaidAmountCent(0L);
        order.setExpireAt(expireAt);
        order.setVersion(0L);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setEventId(snapshot.eventId());
        item.setEventTitle(snapshot.eventTitle());
        item.setSessionId(snapshot.sessionId());
        item.setSessionStartAt(snapshot.sessionStartAt());
        item.setVenueName(snapshot.venueName());
        item.setTierId(snapshot.tierId());
        item.setTierName(snapshot.tierName());
        item.setUnitPriceCent(snapshot.unitPriceCent());
        item.setQuantity(snapshot.quantity());
        item.setAmountCent(amount);
        orderItemMapper.insert(item);

        // 写订单创建流水（业务审计）
        orderOperationMapper.insert(OrderOperation.of(
                order.getId(), OrderOperationType.ORDER_CREATED,
                null, OrderStatus.PENDING_PAYMENT.name(), userId, null));

        return order;
    }

    /** 我的订单分页（按创建时间倒序，可按状态筛选）。 */
    public PageResult<OrderResponse> listMyOrders(Long userId, String status, long page, long pageSize) {
        Page<Order> p = new Page<>(page, pageSize);
        IPage<Order> result = orderMapper.selectUserPage(p, userId, trimToNull(status));
        return new PageResult<>(buildResponses(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 我的订单详情（归属校验，防止越权访问他人订单）。 */
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED);
        }
        return toResponse(order, orderItemMapper.findByOrderId(orderId));
    }

    /** 按抢票请求号查询订单详情（供抢票编排层幂等复用，不存在返回 null）。 */
    public OrderResponse getByRequestId(String requestId) {
        Order order = orderMapper.findByRequestId(requestId);
        if (order == null) {
            return null;
        }
        return toResponse(order, orderItemMapper.findByOrderId(order.getId()));
    }

    /** 管理端订单分页。 */
    public PageResult<OrderResponse> listAdminOrders(String status, String orderNo, Long userId,
                                                     long page, long pageSize) {
        Page<Order> p = new Page<>(page, pageSize);
        IPage<Order> result = orderMapper.selectAdminPage(p,
                trimToNull(status), trimToNull(orderNo), userId);
        return new PageResult<>(buildResponses(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 管理端订单详情。 */
    public OrderResponse getOrderForAdmin(Long orderId) {
        Order order = requireOrder(orderId);
        return toResponse(order, orderItemMapper.findByOrderId(orderId));
    }

    /** 我的订单操作流水（归属校验，防止越权查看他人订单历史）。 */
    public List<OrderOperationResponse> listOperations(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED);
        }
        return orderOperationMapper.findByOrderId(orderId).stream()
                .map(OrderService::toOperationResponse)
                .toList();
    }

    /**
     * 支付编排层读取订单实体（M10，不做归属校验，由调用方校验）。
     */
    public Order getOrderEntity(Long orderId) {
        return requireOrder(orderId);
    }

    /** 支付编排层读取订单明细快照（M10，供权益生成）。 */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.findByOrderId(orderId);
    }

    /**
     * 条件支付确认（M10）：仅 PENDING_PAYMENT 且未过期时转为 PAID。
     *
     * @return true 表示抢到订单状态转换权；false 表示已被并发关单/支付或已过期。
     *         与 {@link OrderCloseService} 的关单条件更新互斥，保证唯一合法终态。
     */
    public boolean markPaid(Long orderId, Long paidAmountCent, LocalDateTime paidAt) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return orderMapper.paidById(orderId, paidAmountCent, paidAt, now) == 1;
    }

    /**
     * 追加订单操作流水（M10 支付编排层记录 PAYMENT_CONFIRMED）。
     */
    public void appendOperation(Long orderId, OrderOperationType type,
                                String fromStatus, String toStatus,
                                Long actorId, String reason) {
        orderOperationMapper.insert(OrderOperation.of(
                orderId, type, fromStatus, toStatus, actorId, reason));
    }

    /* ---------- 内部 ---------- */

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 批量聚合明细，避免列表 N+1。 */
    private List<OrderResponse> buildResponses(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderItemMapper.findByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
                .map(o -> toResponse(o, itemsByOrder.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    /**
     * 业务订单号：FT + 毫秒时间戳 + 4 位随机数（26 位内，唯一约束兜底并发冲突）。
     */
    private String generateOrderNo() {
        return "FT" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static OrderResponse toResponse(Order o, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderService::toItemResponse)
                .toList();
        return new OrderResponse(
                String.valueOf(o.getId()), o.getOrderNo(), o.getRequestId(),
                String.valueOf(o.getUserId()), o.getStatus(),
                o.getOriginalAmountCent(), o.getDiscountAmountCent(), o.getPayableAmountCent(),
                o.getPaidAmountCent(), o.getExpireAt(), o.getPaidAt(), o.getClosedAt(),
                o.getCloseReason(), o.getVersion(), o.getCreatedAt(), o.getUpdatedAt(),
                itemResponses);
    }

    private static OrderItemResponse toItemResponse(OrderItem i) {
        return new OrderItemResponse(
                String.valueOf(i.getId()), String.valueOf(i.getOrderId()),
                String.valueOf(i.getEventId()), i.getEventTitle(),
                String.valueOf(i.getSessionId()), i.getSessionStartAt(), i.getVenueName(),
                String.valueOf(i.getTierId()), i.getTierName(), i.getUnitPriceCent(),
                i.getQuantity(), i.getAmountCent());
    }

    private static OrderOperationResponse toOperationResponse(OrderOperation o) {
        return new OrderOperationResponse(
                String.valueOf(o.getId()), String.valueOf(o.getOrderId()),
                o.getOperation(), o.getFromStatus(), o.getToStatus(),
                o.getActorId() == null ? null : String.valueOf(o.getActorId()),
                o.getReason(), o.getOperationId(), o.getCreatedAt());
    }
}
