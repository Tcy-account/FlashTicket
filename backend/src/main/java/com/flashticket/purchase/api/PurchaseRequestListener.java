package com.flashticket.purchase.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.messaging.dto.PurchaseSubmittedEvent;
import com.flashticket.purchase.service.PurchaseRequestProcessor;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * 抢票消息 PushConsumer（M12）。
 *
 * <p>消费 ft-purchase 事件，解析载荷后交给 {@link PurchaseRequestProcessor} 处理。
 * topic/tag/endpoints 由注解默认占位符从配置读取（rocketmq.push-consumer.*），
 * consumerGroup 显式声明。业务确定结果返回 SUCCESS（不重试），技术故障返回
 * FAILURE（触发 MQ 重试 + 恢复扫描器兜底）。
 *
 * <p>test profile 不创建本监听器，消费正确性由直接调用 Processor 验证。
 */
@Component
@Profile("!test")
@RocketMQMessageListener(
        consumerGroup = PurchaseRequestProcessor.CONSUMER_NAME,
        sslEnabled = false)
public class PurchaseRequestListener implements RocketMQListener {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRequestListener.class);

    private final PurchaseRequestProcessor processor;
    private final ObjectMapper objectMapper;

    public PurchaseRequestListener(PurchaseRequestProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ConsumeResult consume(MessageView messageView) {
        try {
            ByteBuffer buffer = messageView.getBody();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            PurchaseSubmittedEvent event = objectMapper.readValue(bytes, PurchaseSubmittedEvent.class);
            boolean handled = processor.process(event.eventId(), event.requestId());
            return handled ? ConsumeResult.SUCCESS : ConsumeResult.FAILURE;
        } catch (Exception e) {
            log.error("抢票消息消费异常 messageId={}: {}", messageView.getMessageId(), e.getMessage(), e);
            return ConsumeResult.FAILURE;
        }
    }
}
