package com.flashticket.refund.dto;

import java.time.LocalDateTime;

/**
 * 退款业务单响应（雪花 ID 一律 String 传输）。
 *
 * @param id             退款单 ID
 * @param refundNo       退款业务号
 * @param refundRequestId 退款申请幂等号
 * @param paymentId      原支付单 ID
 * @param orderId        原订单 ID
 * @param userId         退款申请人 ID
 * @param amountCent     退款金额（分）
 * @param currency       币种
 * @param reasonCode     退款原因
 * @param reasonDetail   补充说明
 * @param status         退款状态
 * @param approvedBy     审核人 ID（未审核为 null）
 * @param approvedAt     审核通过时间
 * @param rejectedBy     拒绝人 ID
 * @param rejectedAt     拒绝时间
 * @param reviewComment  审核意见
 * @param succeededAt    退款完成时间
 * @param failedAt       最终失败时间
 * @param createdAt      创建时间（UTC）
 * @param updatedAt      更新时间（UTC）
 */
public record RefundResponse(
        String id,
        String refundNo,
        String refundRequestId,
        String paymentId,
        String orderId,
        String userId,
        Long amountCent,
        String currency,
        String reasonCode,
        String reasonDetail,
        String status,
        String approvedBy,
        LocalDateTime approvedAt,
        String rejectedBy,
        LocalDateTime rejectedAt,
        String reviewComment,
        LocalDateTime succeededAt,
        LocalDateTime failedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
