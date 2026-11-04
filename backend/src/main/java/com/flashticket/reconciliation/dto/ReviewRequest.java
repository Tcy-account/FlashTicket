package com.flashticket.reconciliation.dto;

/**
 * 异常工单复核请求。
 */
public record ReviewRequest(
        String resolution,
        String comment
) {
}
