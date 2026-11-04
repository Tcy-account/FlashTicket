package com.flashticket.reconciliation.dto;

import com.flashticket.reconciliation.enums.ReconcileSeverity;

/**
 * 对账发现的单条差异（内部传递记录，落库前由 Service 转成工单）。
 */
public record ReconcileIssue(
        String checkType,
        ReconcileSeverity severity,
        String entityType,
        String entityId,
        String description,
        String evidence
) {
}
