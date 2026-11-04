package com.flashticket.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.identity.entity.OperationAudit;
import com.flashticket.identity.mapper.OperationAuditMapper;
import org.springframework.stereotype.Service;

/**
 * 后台敏感操作审计服务：同步写入 ft_operation_audit。
 *
 * <p>设计约束（见 doc/用户与账号模块设计.md 十）：
 * <ul>
 *   <li>只记录操作员、目标与变更摘要，不记录密码、Token、验证码等敏感内容；</li>
 *   <li>detail 为 JSON 快照，序列化失败降级为 null，不抛异常阻断主流程；</li>
 *   <li>插入随上层编排服务的本地事务提交，保证「关键权限修改」与审计可靠关联。</li>
 * </ul>
 */
@Service
public class OperationAuditService {

    private final OperationAuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public OperationAuditService(OperationAuditMapper auditMapper, ObjectMapper objectMapper) {
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    /** 记录一条后台敏感操作审计。 */
    public void record(Long operatorId, String operatorUsername, String action,
                       String targetType, Long targetId, Object detail, String ip) {
        OperationAudit audit = new OperationAudit();
        audit.setOperatorId(operatorId);
        audit.setOperatorUsername(operatorUsername);
        audit.setAction(action);
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setDetail(toJson(detail));
        audit.setIp(ip);
        auditMapper.insert(audit);
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            return null;
        }
    }
}
