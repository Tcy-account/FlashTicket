package com.flashticket.purchase.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.purchase.dto.PurchaseStatusResponse;
import com.flashticket.purchase.service.PurchaseQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台抢票请求查询与重试入口（M12）。
 *
 * <p>purchase:read 查询请求，purchase:retry 对可恢复请求补发重试。重试复用原
 * requestId，不绕过原限购与预占记录。
 */
@RestController
@RequestMapping("/api/v1/admin/purchases")
public class AdminPurchaseController {

    private final PurchaseQueryService queryService;

    public AdminPurchaseController(PurchaseQueryService queryService) {
        this.queryService = queryService;
    }

    /** 抢票请求分页（可按状态筛选）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('purchase:read')")
    public ApiResponse<PageResult<PurchaseStatusResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(queryService.listAdmin(status, page, pageSize));
    }

    /** 抢票请求详情。 */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('purchase:read')")
    public ApiResponse<PurchaseStatusResponse> detail(@PathVariable String requestId) {
        return ApiResponse.ok(queryService.getAdmin(requestId));
    }

    /** 重试可恢复请求（purchase:retry）。 */
    @PostMapping("/{requestId}/retry")
    @PreAuthorize("hasAuthority('purchase:retry')")
    public ApiResponse<PurchaseStatusResponse> retry(@PathVariable String requestId) {
        return ApiResponse.ok(queryService.retry(requestId));
    }
}
