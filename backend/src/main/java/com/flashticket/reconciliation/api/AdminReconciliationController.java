package com.flashticket.reconciliation.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.reconciliation.dto.ReviewRequest;
import com.flashticket.reconciliation.entity.ReconcileAction;
import com.flashticket.reconciliation.entity.ReconcileException;
import com.flashticket.reconciliation.entity.ReconcileJob;
import com.flashticket.reconciliation.enums.ReconcileScope;
import com.flashticket.reconciliation.service.ReconcileAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统对账后台入口（M16-C）：对账任务 + 异常工单管理。
 */
@RestController
@RequestMapping("/api/v1/admin/reconciliation")
public class AdminReconciliationController {

    private final ReconcileAdminService adminService;

    public AdminReconciliationController(ReconcileAdminService adminService) {
        this.adminService = adminService;
    }

    /** 创建并执行一次对账任务（reconciliation:execute）。 */
    @PostMapping("/jobs")
    @PreAuthorize("hasAuthority('reconciliation:execute')")
    public ApiResponse<ReconcileJob> createJob(@RequestParam ReconcileScope scope) {
        return ApiResponse.ok(adminService.execute(scope, SecurityUtils.currentUserId()));
    }

    /** 对账任务列表（reconciliation:read）。 */
    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<PageResult<ReconcileJob>> listJobs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(adminService.listJobs(page, size));
    }

    /** 对账任务详情（reconciliation:read）。 */
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<ReconcileJob> getJob(@PathVariable("id") Long id) {
        return ApiResponse.ok(adminService.getJob(id));
    }

    /** 异常工单列表（reconciliation:read）。 */
    @GetMapping("/exceptions")
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<PageResult<ReconcileException>> listExceptions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(adminService.listExceptions(status, page, size));
    }

    /** 异常工单详情（reconciliation:read）。 */
    @GetMapping("/exceptions/{id}")
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<ReconcileException> getException(@PathVariable("id") Long id) {
        return ApiResponse.ok(adminService.getException(id));
    }

    /** 异常工单处理记录（reconciliation:read）。 */
    @GetMapping("/exceptions/{id}/actions")
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<List<ReconcileAction>> listActions(@PathVariable("id") Long id) {
        return ApiResponse.ok(adminService.listActions(id));
    }

    /** 提交复核结论（reconciliation:resolve）。 */
    @PostMapping("/exceptions/{id}/review")
    @PreAuthorize("hasAuthority('reconciliation:resolve')")
    public ApiResponse<ReconcileException> review(@PathVariable("id") Long id,
                                                  @RequestBody ReviewRequest request) {
        return ApiResponse.ok(adminService.review(id, request.resolution(), request.comment(),
                SecurityUtils.currentUserId()));
    }
}
