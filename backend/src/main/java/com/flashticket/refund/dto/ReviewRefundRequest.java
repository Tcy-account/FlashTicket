package com.flashticket.refund.dto;

import jakarta.validation.constraints.Size;

/**
 * 财务退款审核请求（POST /api/v1/admin/refunds/{id}/approve|reject）。
 *
 * @param comment 审核意见（审核拒绝时必填）
 */
public record ReviewRefundRequest(
        @Size(max = 500) String comment) {
}
