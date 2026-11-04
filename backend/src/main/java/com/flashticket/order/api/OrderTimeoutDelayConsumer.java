package com.flashticket.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.common.exception.BizException;
import com.flashticket.messaging.dto.OrderCloseDelayEvent;
import com.flashticket.order.service.OrderCloseService;
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
 * 订单支付超时延迟关单消费者（M12）。
 *
 * <p>消费建单成功后发送的 Delay 关单消息，到期后调用 {@link OrderCloseService#closeByTimeout}
 * 关单。关单本身幂等（已支付/已关闭/订单不存在均安全），故无论结果如何都返回
 * SUCCESS 不重试，避免重复触发；M09 的 DB 兜底扫描器保证最终关单正确性。
 *
 * <p>topic/tag 走注解占位符（rocketmq.order-close.*），独立于抢票 topic
 * ft-purchase，避免两类消息串扰。test profile 不创建。
 */
@Component
@Profile("!test")
@RocketMQMessageListener(
        consumerGroup = "ft-order-close-cg",
        topic = "${rocketmq.order-close.topic:ft-order-close}",
        tag = "${rocketmq.order-close.tag:ORDER_CLOSE}",
        sslEnabled = false)
public class OrderTimeoutDelayConsumer implements RocketMQListener {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutDelayConsumer.class);

    private final OrderCloseService orderCloseService;
    private final ObjectMapper objectMapper;

    public OrderTimeoutDelayConsumer(OrderCloseService orderCloseService, ObjectMapper objectMapper) {
        this.orderCloseService = orderCloseService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ConsumeResult consume(MessageView messageView) {
        try {
            ByteBuffer buffer = messageView.getBody();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            OrderCloseDelayEvent event = objectMapper.readValue(bytes, OrderCloseDelayEvent.class);
            orderCloseService.closeByTimeout(event.orderId());
            return ConsumeResult.SUCCESS;
        } catch (BizException e) {
            // 订单已支付/已关闭/不存在：幂等，视为处理完成
            log.debug("延迟关单跳过: {}", e.getErrorCode().getCode());
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            // 解析失败或技术故障：不重试，交由 DB 兜底扫描器保证最终关单
            log.error("延迟关单消费异常 messageId={}: {}", messageView.getMessageId(), e.getMessage(), e);
            return ConsumeResult.SUCCESS;
        }
    }
}
