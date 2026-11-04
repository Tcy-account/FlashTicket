package com.flashticket.risk.dto;

import java.time.LocalDateTime;

/**
 * 风控规则响应。
 */
public record RiskRuleResponse(
        String id,
        String ruleCode,
        String name,
        String scene,
        String action,
        String config,
        int priority,
        String status,
        long version,
        LocalDateTime createdAt
) {
}
