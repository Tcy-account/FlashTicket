package com.flashticket.payment.dto;

import java.time.LocalDateTime;

/**
 * 支付业务单响应（雪花 ID 一律 String 传输）。
 *
 * @param id          支付单 ID
 * @param paymentNo   支付业务编号
 * @param orderId     所属订单 ID
 * @param userId      支付用户 ID
 * @param channel     模拟渠道
 * @param amountCent  应付金额（分）
 * @param currency    币种
 * @param status      支付单状态
 * @param expireAt    支付截止时间（UTC）
 * @param succeededAt 确认成功时间（未成功为 null）
 * @param createdAt   创建时间（UTC）
 */
public record PaymentResponse(
        String id,
        String paymentNo,
        String orderId,
        String userId,
        String channel,
        Long amountCent,
        String currency,
        String status,
        LocalDateTime expireAt,
        LocalDateTime succeededAt,
        LocalDateTime createdAt) {
}
