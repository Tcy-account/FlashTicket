package com.flashticket.messaging.dto;

/**
 * 抢票提交事件载荷（ft_outbox_event.payload 的 JSON 结构，schemaVersion=1）。
 *
 * <p>eventId 为 MQ 业务事件标识（消费 Inbox 去重键），requestId 为抢票请求号。
 * 二者不能混用：一个请求可经历多次消息投递/补发（不同 eventId），但 requestId
 * 唯一，最多产生一个有效订单。
 */
public record PurchaseSubmittedEvent(String eventId, String requestId, int schemaVersion) {

    public static PurchaseSubmittedEvent of(String eventId, String requestId) {
        return new PurchaseSubmittedEvent(eventId, requestId, 1);
    }
}
