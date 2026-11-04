package com.flashticket.messaging.service;

import com.flashticket.messaging.dto.PurchaseSubmittedEvent;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * RocketMQ 5.x gRPC 发送器（M12）。
 *
 * <p>封装 RocketMQClientTemplate（自动配置在配置了 rocketmq.producer.endpoints 时
 * 创建，懒连接 Proxy）。test profile 不创建本组件，集成测试不依赖真实 MQ，
 * 收发正确性由本地 docker compose 运行验证。
 */
@Component
@Profile("!test")
public class RocketMQPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMQPublisher.class);

    private final RocketMQClientTemplate template;
    private final ObjectMapper objectMapper;

    public RocketMQPublisher(RocketMQClientTemplate template, ObjectMapper objectMapper) {
        this.template = template;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送普通消息到指定 topic 与 tag（destination 形如 topic:tag）。
     */
    public void send(String topic, String tag, PurchaseSubmittedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            template.syncSendNormalMessage(topic + ":" + tag, payload);
        } catch (Exception e) {
            log.error("发送抢票事件失败 topic={}, eventId={}", topic, event.eventId(), e);
            throw new IllegalStateException("RocketMQ send failed", e);
        }
    }

    /**
     * 发送原始 JSON 字符串到指定 topic 与 tag（M14 泛化：Outbox 载荷原样转发，
     * 发布器无需反序列化成具体 Java 类型，天然支持任意事件类型）。
     */
    public void sendRaw(String topic, String tag, String payload) {
        try {
            template.syncSendNormalMessage(topic + ":" + tag, payload);
        } catch (Exception e) {
            log.error("发送消息失败 topic={}, tag={}", topic, tag, e);
            throw new IllegalStateException("RocketMQ send failed", e);
        }
    }

    /**
     * 发送延迟消息（RocketMQ Delay）到指定 topic 与 tag，投递时间 = 发送时刻 + delay。
     * 用于订单支付超时关单等「到点触发」场景。
     */
    public void sendDelay(String topic, String tag, Object event, Duration delay) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            template.syncSendDelayMessage(topic + ":" + tag, payload, delay);
        } catch (Exception e) {
            log.error("发送延迟消息失败 topic={}, tag={}", topic, tag, e);
            throw new IllegalStateException("RocketMQ delay send failed", e);
        }
    }
}
