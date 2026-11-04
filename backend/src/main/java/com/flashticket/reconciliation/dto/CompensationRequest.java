package com.flashticket.reconciliation.dto;

/**
 * 创建补偿任务请求。
 */
public record CompensationRequest(
        String taskType,
        String entityType,
        String entityId,
        Long exceptionId
) {
}
