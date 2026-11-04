package com.flashticket.identity.dto;

import java.time.LocalDateTime;

/**
 * 后台敏感操作审计项（雪花 ID 以字符串传输）。
 */
public record AuditLogResponse(
        String id,
        String operatorId,
        String operatorUsername,
        String action,
        String targetType,
        String targetId,
        String detail,
        String ip,
        LocalDateTime createdAt) {
}
