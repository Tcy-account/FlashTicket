package com.flashticket.risk.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建/更新风控规则请求（M15-B）。
 */
public record RiskRuleRequest(
        @NotBlank String ruleCode,
        @NotBlank String name,
        @NotBlank String scene,
        @NotBlank String action,
        String config,
        int priority
) {
}
