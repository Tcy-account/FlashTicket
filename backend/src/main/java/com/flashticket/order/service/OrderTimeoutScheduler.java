package com.flashticket.order.service;

import com.flashticket.common.exception.BizException;
import com.flashticket.order.entity.Order;
import com.flashticket.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 支付超时关单定时扫描器（M09）。
 *
 * <p>作为超时关单的数据库兜底（设计上 RocketMQ Delay 为第一层，M09 尚未接入，
 * 故以 Spring Scheduling 扫描为基线实现，见 doc/订单与交易状态机模块.md 6.1）：
 * 周期性扫描「仍为 PENDING_PAYMENT 且支付期限已到」的订单，逐个调用关单服务。
 *
 * <p>每个订单独立事务关闭，单个失败不影响整批；重复调度/并发关单由条件更新保证
 * 幂等（不重复释放库存）。test profile 下关闭调度，避免集成测试期间异步干扰。
 */
@Component
@Profile("!test")
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final OrderMapper orderMapper;
    private final OrderCloseService orderCloseService;
    private final int batchSize;

    public OrderTimeoutScheduler(OrderMapper orderMapper,
                                 OrderCloseService orderCloseService,
                                 @Value("${flashticket.order.close-scan-batch:100}") int batchSize) {
        this.orderMapper = orderMapper;
        this.orderCloseService = orderCloseService;
        this.batchSize = batchSize;
    }

    /** 每隔一段固定延迟扫描一批过期订单并关单（首轮应用启动后立即执行一次）。 */
    @Scheduled(fixedDelayString = "${flashticket.order.close-scan-delay-ms:30000}",
            initialDelayString = "${flashticket.order.close-scan-delay-ms:30000}")
    public void scanAndCloseExpired() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Order> expired = orderMapper.selectExpiredPending(now, batchSize);
        if (expired.isEmpty()) {
            return;
        }
        for (Order order : expired) {
            try {
                orderCloseService.closeByTimeout(order.getId());
            } catch (BizException e) {
                // 并发已被支付/关闭或状态异常，忽略继续下一单
                log.debug("跳过过期订单关单 id={}, reason={}", order.getId(), e.getErrorCode().getCode());
            }
        }
    }
}
