package com.flashticket.order.service;

import com.flashticket.messaging.dto.OrderCloseDelayEvent;
import com.flashticket.messaging.service.RocketMQPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RocketMQ 实现：发送订单支付超时延迟关单消息（M12）。
 *
 * <p>使用独立 topic 与 tag，延迟投递到 {@code OrderTimeoutDelayConsumer}。
 * 发送失败仅记录日志不抛出——M09 的 DB 兜底扫描器保证最终关单，Delay 消息
 * 只是把「15 分钟关单」做得更准时，不是正确性依赖。test profile 不创建。
 */
@Component
@Profile("!test")
public class RocketMQOrderCloseDelaySender implements OrderCloseDelaySender {

    private static final Logger log = LoggerFactory.getLogger(RocketMQOrderCloseDelaySender.class);

    private final RocketMQPublisher publisher;
    private final String topic;
    private final String tag;

    public RocketMQOrderCloseDelaySender(RocketMQPublisher publisher,
                                         @Value("${rocketmq.order-close.topic:ft-order-close}") String topic,
                                         @Value("${rocketmq.order-close.tag:ORDER_CLOSE}") String tag) {
        this.publisher = publisher;
        this.topic = topic;
        this.tag = tag;
    }

    @Override
    public void scheduleClose(Long orderId, Duration delay) {
        try {
            publisher.sendDelay(topic, tag, OrderCloseDelayEvent.of(orderId), delay);
        } catch (Exception e) {
            log.warn("延迟关单消息发送失败 orderId={}, 将由 DB 兜底扫描关单: {}",
                    orderId, e.getMessage());
        }
    }
}
