package com.flashticket.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.risk.dto.RiskRuleRequest;
import com.flashticket.risk.dto.RiskRuleResponse;
import com.flashticket.risk.entity.RiskOperation;
import com.flashticket.risk.entity.RiskRule;
import com.flashticket.risk.mapper.RiskOperationMapper;
import com.flashticket.risk.mapper.RiskRuleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 风控规则服务（M15-B）：规则 CRUD、启停与审计。
 *
 * <p>P0 为确定性规则定义（场景 + 动作 + JSON 配置），实际限流执行仍由
 * {@link RateLimitService} 的 Lua 完成；规则表用于可管理、可审计的规则配置，
 * 完整动态规则引擎（版本快照、评分）留 P1。
 */
@Service
public class RiskRuleService {

    private final RiskRuleMapper ruleMapper;
    private final RiskOperationMapper operationMapper;

    public RiskRuleService(RiskRuleMapper ruleMapper, RiskOperationMapper operationMapper) {
        this.ruleMapper = ruleMapper;
        this.operationMapper = operationMapper;
    }

    /** 创建规则。 */
    @Transactional
    public RiskRuleResponse create(Long operatorId, RiskRuleRequest request) {
        RiskRule rule = new RiskRule();
        rule.setRuleCode(request.ruleCode());
        rule.setName(request.name());
        rule.setScene(request.scene());
        rule.setAction(request.action());
        rule.setConfig(request.config());
        rule.setPriority(request.priority());
        rule.setStatus("ENABLED");
        rule.setVersion(0L);
        rule.setCreatedBy(operatorId);
        try {
            ruleMapper.insert(rule);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.RISK_RULE_CODE_TAKEN);
        }
        writeOperation(operatorId, "RULE", String.valueOf(rule.getId()), "CREATE", null);
        return toResponse(rule);
    }

    /** 更新规则（保持 version +1）。 */
    @Transactional
    public RiskRuleResponse update(Long operatorId, Long id, RiskRuleRequest request) {
        RiskRule rule = requireRule(id);
        rule.setRuleCode(request.ruleCode());
        rule.setName(request.name());
        rule.setScene(request.scene());
        rule.setAction(request.action());
        rule.setConfig(request.config());
        rule.setPriority(request.priority());
        rule.setVersion(rule.getVersion() + 1);
        try {
            ruleMapper.updateById(rule);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.RISK_RULE_CODE_TAKEN);
        }
        writeOperation(operatorId, "RULE", String.valueOf(id), "UPDATE", null);
        return toResponse(ruleMapper.selectById(id));
    }

    /** 启停规则。 */
    @Transactional
    public RiskRuleResponse toggle(Long operatorId, Long id, boolean enabled) {
        RiskRule rule = requireRule(id);
        rule.setStatus(enabled ? "ENABLED" : "DISABLED");
        ruleMapper.updateById(rule);
        writeOperation(operatorId, "RULE", String.valueOf(id), enabled ? "ENABLE" : "DISABLE", null);
        return toResponse(ruleMapper.selectById(id));
    }

    /** 规则列表。 */
    public List<RiskRuleResponse> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<RiskRule>().orderByAsc(RiskRule::getPriority))
                .stream().map(RiskRuleService::toResponse).toList();
    }

    /* ---------- 内部 ---------- */

    private RiskRule requireRule(Long id) {
        RiskRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RISK_RULE_NOT_FOUND);
        }
        return rule;
    }

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

    private static RiskRuleResponse toResponse(RiskRule r) {
        return new RiskRuleResponse(
                String.valueOf(r.getId()), r.getRuleCode(), r.getName(), r.getScene(),
                r.getAction(), r.getConfig(), r.getPriority() == null ? 0 : r.getPriority(),
                r.getStatus(), r.getVersion() == null ? 0 : r.getVersion(), r.getCreatedAt());
    }
}
