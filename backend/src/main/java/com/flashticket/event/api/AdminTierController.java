package com.flashticket.event.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.event.dto.SalePauseRequest;
import com.flashticket.event.dto.SalePlanRequest;
import com.flashticket.event.dto.SalePlanResponse;
import com.flashticket.event.dto.TicketTierRequest;
import com.flashticket.event.dto.TicketTierResponse;
import com.flashticket.event.service.SalePlanService;
import com.flashticket.event.service.TicketTierService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台票档与售票计划管理入口（M06）。
 *
 * <p>票档配置、售票计划、暂停/恢复属演出域日常运营，OPERATOR / SUPER_ADMIN
 * 持有对应权限点（见 doc/演出活动与场次.md 九）。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminTierController {

    private final TicketTierService tierService;
    private final SalePlanService salePlanService;

    public AdminTierController(TicketTierService tierService,
                               SalePlanService salePlanService) {
        this.tierService = tierService;
        this.salePlanService = salePlanService;
    }

    /** 某场次的票档列表（含售票计划）。 */
    @GetMapping("/sessions/{sessionId}/tiers")
    @PreAuthorize("hasAuthority('event:tier:list')")
    public ApiResponse<List<TicketTierResponse>> list(@PathVariable Long sessionId) {
        return ApiResponse.ok(tierService.listBySession(sessionId));
    }

    /** 创建票档（容量约束校验）。 */
    @PostMapping("/sessions/{sessionId}/tiers")
    @PreAuthorize("hasAuthority('event:tier:create')")
    public ApiResponse<TicketTierResponse> create(@PathVariable Long sessionId,
                                                  @Valid @RequestBody TicketTierRequest request,
                                                  HttpServletRequest http) {
        return ApiResponse.ok(tierService.create(
                sessionId, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 编辑票档（版本校验）。 */
    @PutMapping("/tiers/{id}")
    @PreAuthorize("hasAuthority('event:tier:update')")
    public ApiResponse<TicketTierResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody TicketTierRequest request,
                                                  HttpServletRequest http) {
        return ApiResponse.ok(tierService.update(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 配置售票计划（开售/结束时间）。 */
    @PutMapping("/tiers/{id}/sale-plan")
    @PreAuthorize("hasAuthority('event:sale-plan:update')")
    public ApiResponse<SalePlanResponse> configureSalePlan(@PathVariable Long id,
                                                           @Valid @RequestBody SalePlanRequest request,
                                                           HttpServletRequest http) {
        return ApiResponse.ok(salePlanService.configure(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 暂停销售。 */
    @PostMapping("/tiers/{id}/pause")
    @PreAuthorize("hasAuthority('event:sale-plan:pause')")
    public ApiResponse<SalePlanResponse> pause(@PathVariable Long id,
                                               @Valid @RequestBody SalePauseRequest request,
                                               HttpServletRequest http) {
        return ApiResponse.ok(salePlanService.pause(
                id, request.reason(), SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 恢复销售。 */
    @PostMapping("/tiers/{id}/resume")
    @PreAuthorize("hasAuthority('event:sale-plan:resume')")
    public ApiResponse<SalePlanResponse> resume(@PathVariable Long id,
                                                HttpServletRequest http) {
        return ApiResponse.ok(salePlanService.resume(
                id, SecurityUtils.currentUser(), clientIp(http)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
