package com.flashticket.risk.dto;

import java.time.LocalDateTime;

/**
 * 风控操作审计记录响应。
 */
public record RiskOperationResponse(
        String id,
        String operatorId,
        String targetType,
        String targetId,
        String action,
        String reason,
        LocalDateTime createdAt
) {
}
