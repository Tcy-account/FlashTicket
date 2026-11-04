package com.flashticket.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 创建风控临时限制请求（M15-B）。
 */
public record RestrictionRequest(
        @NotBlank String subjectType,
        @NotBlank String subjectId,
        @NotBlank String scope,
        @NotBlank String reason,
        @NotNull LocalDateTime expireAt
) {
}
