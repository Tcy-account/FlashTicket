package com.flashticket.reconciliation.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.reconciliation.dto.CompensationRequest;
import com.flashticket.reconciliation.entity.CompensationTask;
import com.flashticket.reconciliation.service.CompensationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 补偿任务后台入口（M16-D）：创建 + 执行安全补偿。
 *
 * <p>创建/执行需 compensation:execute（超管）；查询走 reconciliation:read。
 */
@RestController
@RequestMapping("/api/v1/admin/compensations")
public class AdminCompensationController {

    private final CompensationService compensationService;

    public AdminCompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    /** 创建补偿任务（compensation:execute）。 */
    @PostMapping
    @PreAuthorize("hasAuthority('compensation:execute')")
    public ApiResponse<CompensationTask> create(@RequestBody CompensationRequest request) {
        return ApiResponse.ok(compensationService.create(request.taskType(),
                request.entityType(), request.entityId(), request.exceptionId()));
    }

    /** 执行补偿任务（compensation:execute）。 */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('compensation:execute')")
    public ApiResponse<CompensationTask> execute(@PathVariable("id") Long id) {
        return ApiResponse.ok(compensationService.execute(id));
    }

    /** 补偿任务列表（reconciliation:read）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<PageResult<CompensationTask>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(compensationService.list(page, size));
    }

    /** 补偿任务详情（reconciliation:read）。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public ApiResponse<CompensationTask> get(@PathVariable("id") Long id) {
        return ApiResponse.ok(compensationService.get(id));
    }
}
