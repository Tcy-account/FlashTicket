package com.flashticket.risk.dto;

import java.time.LocalDateTime;

/**
 * 风控临时限制响应。
 */
public record RestrictionResponse(
        String id,
        String subjectType,
        String subjectId,
        String scope,
        String reason,
        LocalDateTime expireAt,
        String status,
        String createdBy,
        String revokedBy,
        LocalDateTime revokedAt,
        LocalDateTime createdAt
) {
}
