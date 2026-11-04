package com.flashticket.order.service;

import java.time.Duration;

/**
 * 订单支付超时延迟关单发送器（M12）。
 *
 * <p>建单成功后按支付期限安排一条 RocketMQ Delay 消息，作为超时关单的第一层
 * 精确触发；M09 的 {@link OrderTimeoutScheduler} DB 兜底扫描保留为最终保障，
 * 因此本发送器失败/缺失均不影响关单正确性（测试 profile 下无实现）。
 */
public interface OrderCloseDelaySender {

    /**
     * 安排订单在 {@code delay} 后触发支付超时关单。
     *
     * @param orderId 待关订单 ID
     * @param delay   延迟时长（通常为支付期限）
     */
    void scheduleClose(Long orderId, Duration delay);
}
