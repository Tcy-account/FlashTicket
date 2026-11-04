package com.flashticket.reconciliation.enums;

/**
 * 补偿任务状态机：PENDING → RUNNING → SUCCEEDED / RETRY_WAIT / MANUAL_REVIEW / FAILED。
 */
public enum CompensationStatus {
    PENDING, RUNNING, SUCCEEDED, RETRY_WAIT, MANUAL_REVIEW, FAILED
}
