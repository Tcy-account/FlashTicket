package com.flashticket.purchase.service;

import com.flashticket.coupon.dto.CouponLockResult;
import com.flashticket.coupon.service.UserCouponService;
import com.flashticket.inventory.service.InventoryService;
import com.flashticket.messaging.entity.MessageInbox;
import com.flashticket.messaging.mapper.MessageInboxMapper;
import com.flashticket.notification.enums.NotificationEventType;
import com.flashticket.notification.service.NotificationEventPublisher;
import com.flashticket.order.dto.OrderSnapshot;
import com.flashticket.order.service.OrderService;
import com.flashticket.purchase.mapper.PurchaseRequestMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 抢票建单事务服务（M12）。
 *
 * <p>把「库存预占 + 创建订单 + 请求终态 + Inbox 消费记录」放进同一个 MySQL
 * 本地事务：模块化单体内订单与库存共库，不必拆分布式事务。租约失效（被恢复
 * 扫描器收回/被其他消费者抢占）时抛 {@link LeaseLostException} 整体回滚，交由
 * 持有新租约的处理者继续。拆分独立 bean 是为避开 {@code @Transactional} 的
 * self-invocation 失效（见 M09 同坑）。
 */
@Service
public class PurchaseOrderBuilder {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final UserCouponService userCouponService;
    private final PurchaseRequestMapper requestMapper;
    private final MessageInboxMapper inboxMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final int paymentTimeoutMinutes;

    public PurchaseOrderBuilder(InventoryService inventoryService,
                                OrderService orderService,
                                UserCouponService userCouponService,
                                PurchaseRequestMapper requestMapper,
                                MessageInboxMapper inboxMapper,
                                NotificationEventPublisher notificationEventPublisher,
                                @Value("${flashticket.order.payment-timeout-minutes:15}") int paymentTimeoutMinutes) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.userCouponService = userCouponService;
        this.requestMapper = requestMapper;
        this.inboxMapper = inboxMapper;
        this.notificationEventPublisher = notificationEventPublisher;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    /**
     * 建单事务（与消费编排在同一本地事务内）。
     *
     * @return 新建订单 ID
     */
    @Transactional
    public Long build(String requestId, Long userId, int quantity, String leaseOwner,
                      OrderSnapshot snapshot, String eventId, String consumerName,
                      Long selectedCouponId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expireAt = now.plusMinutes(paymentTimeoutMinutes);

        // 1. 库存预占 + 限购占用（条件扣减防超卖，售罄/限购抛 BizException）
        inventoryService.reserve(requestId, snapshot.tierId(), userId, quantity, expireAt);

        // 2. 锁券 + 服务端优惠计算（选了券才执行；券失效/不适用/未达门槛抛 BizException → 抢票失败）
        long discount = 0;
        Long userCouponId = null;
        Long couponTemplateId = null;
        String promotionSnapshot = null;
        if (selectedCouponId != null) {
            CouponLockResult lock = userCouponService.lockAndCalculate(
                    userId, selectedCouponId, snapshot.eventId(), snapshot.tierId(),
                    snapshot.amountCent(), now);
            discount = lock.discountAmount();
            userCouponId = lock.userCouponId();
            couponTemplateId = lock.couponTemplateId();
            promotionSnapshot = lock.promotionSnapshot();
        }

        // 3. 创建订单与明细快照（带优惠金额与规则快照，request_id 唯一兜底防重复下单）
        var order = orderService.createOrder(requestId, userId, snapshot, expireAt,
                discount, userCouponId, couponTemplateId, promotionSnapshot);

        // 4. 落锁券 usage 记录（建单拿到 orderId 后，同一事务）
        if (selectedCouponId != null) {
            userCouponService.recordUsage(userCouponId, order.getId(), now);
        }

        // 5. 标记请求 SUCCEEDED（须仍持有有效租约，失败则整体回滚）
        if (requestMapper.markSucceeded(requestId, order.getId(), leaseOwner, now) == 0) {
            throw new LeaseLostException();
        }

        // 6. Inbox 消费幂等记录（与业务同事务，避免「已消费但未建单」）
        MessageInbox inbox = new MessageInbox();
        inbox.setConsumerName(consumerName);
        inbox.setEventId(eventId);
        inbox.setProcessedAt(now);
        inboxMapper.insert(inbox);

        // 7. 抢票成功通知（与建单同事务写 Outbox，异步消费生成站内消息）
        notificationEventPublisher.publish(NotificationEventType.PURCHASE_SUCCESS,
                userId, String.valueOf(order.getId()));

        return order.getId();
    }
}
