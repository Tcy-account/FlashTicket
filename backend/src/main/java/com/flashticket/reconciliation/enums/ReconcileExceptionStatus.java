package com.flashticket.reconciliation.enums;

/**
 * 异常工单状态机：OPEN → INVESTIGATING → RESOLVED → CLOSED。
 *
 * <p>误报通过 resolution=FALSE_POSITIVE 记录原因，不删除工单。
 */
public enum ReconcileExceptionStatus {
    OPEN, INVESTIGATING, RESOLVED, CLOSED
}
