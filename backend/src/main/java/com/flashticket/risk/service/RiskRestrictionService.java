package com.flashticket.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.risk.dto.RestrictionRequest;
import com.flashticket.risk.dto.RestrictionResponse;
import com.flashticket.risk.entity.RiskOperation;
import com.flashticket.risk.entity.RiskRestriction;
import com.flashticket.risk.enums.RestrictionStatus;
import com.flashticket.risk.mapper.RiskOperationMapper;
import com.flashticket.risk.mapper.RiskRestrictionMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 风控临时限制服务（M15-B）：创建、解除、查询、准入检查与到期自动解除。
 *
 * <p>限制（黑名单）只阻止新的业务请求，不直接禁用账号（账号禁用属账号管理模块）。
 * 准入检查对业务主体按 scope（PURCHASE/CLAIM/LOGIN/ALL）匹配，ALL 对所有业务生效。
 */
@Service
public class RiskRestrictionService {

    private final RiskRestrictionMapper restrictionMapper;
    private final RiskOperationMapper operationMapper;

    public RiskRestrictionService(RiskRestrictionMapper restrictionMapper,
                                  RiskOperationMapper operationMapper) {
        this.restrictionMapper = restrictionMapper;
        this.operationMapper = operationMapper;
    }

    /** 创建临时限制（须在事务内，与审计同提交）。 */
    @Transactional
    public RestrictionResponse create(Long operatorId, RestrictionRequest request) {
        RiskRestriction r = new RiskRestriction();
        r.setSubjectType(request.subjectType());
        r.setSubjectId(request.subjectId());
        r.setScope(request.scope());
        r.setReason(request.reason());
        r.setExpireAt(request.expireAt());
        r.setStatus(RestrictionStatus.ACTIVE.name());
        r.setCreatedBy(operatorId);
        restrictionMapper.insert(r);

        writeOperation(operatorId, "RESTRICTION", String.valueOf(r.getId()), "CREATE", request.reason());
        return toResponse(r);
    }

    /** 提前解除限制（须在事务内，与审计同提交）。 */
    @Transactional
    public RestrictionResponse revoke(Long operatorId, Long id) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (restrictionMapper.revoke(id, operatorId, now) == 0) {
            RiskRestriction latest = restrictionMapper.selectById(id);
            if (latest == null) {
                throw new BizException(ErrorCode.RISK_RESTRICTION_NOT_FOUND);
            }
            return toResponse(latest); // 已解除/已过期，幂等返回
        }
        writeOperation(operatorId, "RESTRICTION", String.valueOf(id), "REVOKE", null);
        return toResponse(restrictionMapper.selectById(id));
    }

    /** 限制列表（按主体/状态过滤）。 */
    public List<RestrictionResponse> list(String subjectType, String subjectId, String status) {
        LambdaQueryWrapper<RiskRestriction> qw = new LambdaQueryWrapper<>();
        if (subjectType != null && !subjectType.isBlank()) {
            qw.eq(RiskRestriction::getSubjectType, subjectType);
        }
        if (subjectId != null && !subjectId.isBlank()) {
            qw.eq(RiskRestriction::getSubjectId, subjectId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(RiskRestriction::getStatus, status);
        }
        qw.orderByDesc(RiskRestriction::getId);
        return restrictionMapper.selectList(qw).stream().map(RiskRestrictionService::toResponse).toList();
    }

    /**
     * 准入检查：主体在 scope 下存在生效限制则拒绝。
     *
     * @param scope 业务范围（PURCHASE/CLAIM/LOGIN）
     */
    public void checkRestriction(String subjectType, String subjectId, String scope) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<RiskRestriction> active = restrictionMapper.selectActiveBySubject(subjectType, subjectId, now);
        for (RiskRestriction r : active) {
            if ("ALL".equals(r.getScope()) || scope.equals(r.getScope())) {
                throw new BizException(ErrorCode.RISK_RESTRICTED, r.getReason());
            }
        }
    }

    /** 到期自动解除（定时扫描 ACTIVE 且已到期 → EXPIRED）。 */
    @Scheduled(fixedDelayString = "${flashticket.risk.restriction-expire-scan-ms:60000}",
            initialDelayString = "${flashticket.risk.restriction-expire-scan-ms:60000}")
    public void expireBatch() {
        restrictionMapper.expireBatch(LocalDateTime.now(ZoneOffset.UTC));
    }

    /* ---------- 内部 ---------- */

    private void writeOperation(Long operatorId, String targetType, String targetId,
                                String action, String reason) {
        RiskOperation op = new RiskOperation();
        op.setOperatorId(operatorId);
        op.setTargetType(targetType);
        op.setTargetId(targetId);
        op.setAction(action);
        op.setReason(reason);
        operationMapper.insert(op);
    }

    private static RestrictionResponse toResponse(RiskRestriction r) {
        return new RestrictionResponse(
                String.valueOf(r.getId()), r.getSubjectType(), r.getSubjectId(), r.getScope(),
                r.getReason(), r.getExpireAt(), r.getStatus(),
                r.getCreatedBy() == null ? null : String.valueOf(r.getCreatedBy()),
                r.getRevokedBy() == null ? null : String.valueOf(r.getRevokedBy()),
                r.getRevokedAt(), r.getCreatedAt());
    }
}
