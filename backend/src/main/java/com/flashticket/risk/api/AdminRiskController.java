package com.flashticket.risk.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.risk.dto.RestrictionRequest;
import com.flashticket.risk.dto.RestrictionResponse;
import com.flashticket.risk.dto.RiskOperationResponse;
import com.flashticket.risk.dto.RiskRuleRequest;
import com.flashticket.risk.dto.RiskRuleResponse;
import com.flashticket.risk.entity.RiskOperation;
import com.flashticket.risk.mapper.RiskOperationMapper;
import com.flashticket.risk.service.RiskRestrictionService;
import com.flashticket.risk.service.RiskRuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台风控管理入口（M15-B）：临时限制/黑名单、风控规则、操作审计。
 *
 * <p>权限：SUPER_ADMIN 管理规则与黑名单，OPERATOR 查询风险与审计。
 */
@RestController
@RequestMapping("/api/v1/admin/risk")
public class AdminRiskController {

    private final RiskRestrictionService restrictionService;
    private final RiskRuleService ruleService;
    private final RiskOperationMapper operationMapper;

    public AdminRiskController(RiskRestrictionService restrictionService,
                               RiskRuleService ruleService,
                               RiskOperationMapper operationMapper) {
        this.restrictionService = restrictionService;
        this.ruleService = ruleService;
        this.operationMapper = operationMapper;
    }

    /* ---------- 临时限制/黑名单 ---------- */

    @GetMapping("/restrictions")
    @PreAuthorize("hasAuthority('risk:read')")
    public ApiResponse<List<RestrictionResponse>> listRestrictions(
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(restrictionService.list(subjectType, subjectId, status));
    }

    @PostMapping("/restrictions")
    @PreAuthorize("hasAuthority('risk:restriction:manage')")
    public ApiResponse<RestrictionResponse> createRestriction(@Valid @RequestBody RestrictionRequest request) {
        return ApiResponse.ok(restrictionService.create(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/restrictions/{id}/revoke")
    @PreAuthorize("hasAuthority('risk:restriction:manage')")
    public ApiResponse<RestrictionResponse> revokeRestriction(@PathVariable Long id) {
        return ApiResponse.ok(restrictionService.revoke(SecurityUtils.currentUserId(), id));
    }

    /* ---------- 风控规则 ---------- */

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('risk:read')")
    public ApiResponse<List<RiskRuleResponse>> listRules() {
        return ApiResponse.ok(ruleService.list());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('risk:rule:manage')")
    public ApiResponse<RiskRuleResponse> createRule(@Valid @RequestBody RiskRuleRequest request) {
        return ApiResponse.ok(ruleService.create(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('risk:rule:manage')")
    public ApiResponse<RiskRuleResponse> updateRule(@PathVariable Long id,
                                                    @Valid @RequestBody RiskRuleRequest request) {
        return ApiResponse.ok(ruleService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping("/rules/{id}/toggle")
    @PreAuthorize("hasAuthority('risk:rule:manage')")
    public ApiResponse<RiskRuleResponse> toggleRule(@PathVariable Long id,
                                                    @RequestParam boolean enabled) {
        return ApiResponse.ok(ruleService.toggle(SecurityUtils.currentUserId(), id, enabled));
    }

    /* ---------- 操作审计 ---------- */

    @GetMapping("/operations")
    @PreAuthorize("hasAuthority('risk:audit:read')")
    public ApiResponse<List<RiskOperationResponse>> listOperations() {
        return ApiResponse.ok(operationMapper.selectList(
                        new LambdaQueryWrapper<RiskOperation>().orderByDesc(RiskOperation::getId).last("LIMIT 200"))
                .stream().map(AdminRiskController::toOperationResponse).toList());
    }

    private static RiskOperationResponse toOperationResponse(RiskOperation o) {
        return new RiskOperationResponse(
                String.valueOf(o.getId()), String.valueOf(o.getOperatorId()),
                o.getTargetType(), o.getTargetId(), o.getAction(), o.getReason(), o.getCreatedAt());
    }
}
