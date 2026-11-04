package com.flashticket.messaging.dto;

/**
 * 订单支付超时延迟关单消息（M12）。
 *
 * <p>建单成功后发送一条 15 分钟延迟消息（RocketMQ Delay），到期后由
 * {@code OrderTimeoutDelayConsumer} 消费并调用关单服务，作为超时关单的第一层
 * 精确触发；M09 的 {@code OrderTimeoutScheduler} DB 兜底扫描保留为最终保障，
 * 因此本消息丢失/重复均不影响正确性（关单幂等）。
 *
 * @param orderId       待关订单 ID
 * @param schemaVersion 消息结构版本
 */
public record OrderCloseDelayEvent(Long orderId, int schemaVersion) {

    public static OrderCloseDelayEvent of(Long orderId) {
        return new OrderCloseDelayEvent(orderId, 1);
    }
}
