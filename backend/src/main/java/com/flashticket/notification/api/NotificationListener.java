package com.flashticket.notification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.notification.dto.NotificationEvent;
import com.flashticket.notification.service.NotificationProcessor;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * 通知消息 PushConsumer（M14 P0-C）。
 *
 * <p>消费 ft-notification 事件，解析载荷后交给 {@link NotificationProcessor}
 * 处理。topic/tag/endpoints 由注解占位符从配置读取（rocketmq.notification.*），
 * consumerGroup 显式声明为 {@code ft-notification-cg}。业务确定结果返回 SUCCESS
 * 不重试；Inbox 唯一约束冲突（并发幂等）视为已处理；技术故障返回 FAILURE 触发
 * MQ 重试。
 *
 * <p>test profile 不创建本监听器，消费正确性由直接调用 Processor 验证。
 */
@Component
@Profile("!test")
@RocketMQMessageListener(
        consumerGroup = NotificationProcessor.CONSUMER_NAME,
        topic = "${rocketmq.notification.topic:ft-notification}",
        tag = "${rocketmq.notification.tag:*}",
        sslEnabled = false)
public class NotificationListener implements RocketMQListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationProcessor processor;
    private final ObjectMapper objectMapper;

    public NotificationListener(NotificationProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ConsumeResult consume(MessageView messageView) {
        try {
            ByteBuffer buffer = messageView.getBody();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            NotificationEvent event = objectMapper.readValue(bytes, NotificationEvent.class);
            processor.process(event);
            return ConsumeResult.SUCCESS;
        } catch (DuplicateKeyException e) {
            // Inbox 唯一约束冲突：并发幂等，视为已处理
            log.debug("通知重复消费被幂等拦截 messageId={}", messageView.getMessageId());
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            // 解析失败或技术故障（DB 不可用等）：由 MQ 重试
            log.error("通知消费异常 messageId={}: {}", messageView.getMessageId(), e.getMessage(), e);
            return ConsumeResult.FAILURE;
        }
    }
}
