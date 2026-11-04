package com.flashticket.coupon.enums;

/**
 * 领券请求状态（表 ft_coupon_claim_request.status）。
 *
 * <p>第一版 MySQL 同步领券只用 SUCCEEDED/FAILED；六态为后续 RocketMQ 异步
 * 发券（Outbox/Inbox/租约/恢复）预留，与抢票请求状态机对齐。
 */
public enum ClaimStatus {
    QUEUED,
    PROCESSING,
    RECOVERING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
