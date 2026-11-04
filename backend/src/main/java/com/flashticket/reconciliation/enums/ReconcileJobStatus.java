package com.flashticket.reconciliation.enums;

/**
 * 对账任务状态机：PENDING → RUNNING → SUCCEEDED / FAILED。
 *
 * <p>SUCCEEDED 仅表示扫描完整执行，发现的差异作为独立工单管理。
 */
public enum ReconcileJobStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED
}
