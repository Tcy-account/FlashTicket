package com.flashticket.coupon.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.coupon.dto.CouponTemplateRequest;
import com.flashticket.coupon.dto.CouponTemplateResponse;
import com.flashticket.coupon.service.CouponTemplateService;
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

/**
 * 后台优惠券模板管理入口（M13）。
 *
 * <p>模板创建/编辑/启停属 coupon:manage。
 */
@RestController
@RequestMapping("/api/v1/admin/coupon-templates")
public class AdminCouponTemplateController {

    private final CouponTemplateService templateService;

    public AdminCouponTemplateController(CouponTemplateService templateService) {
        this.templateService = templateService;
    }

    /** 模板分页列表。 */
    @GetMapping
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<PageResult<CouponTemplateResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long campaignId) {
        return ApiResponse.ok(templateService.list(page, pageSize, status, campaignId));
    }

    /** 创建模板（DRAFT）。 */
    @PostMapping
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<CouponTemplateResponse> create(@Valid @RequestBody CouponTemplateRequest request) {
        return ApiResponse.ok(templateService.create(request));
    }

    /** 编辑模板（仅 DRAFT）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<CouponTemplateResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody CouponTemplateRequest request) {
        return ApiResponse.ok(templateService.update(id, request));
    }

    /** 启用（初始化额度账本）。 */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<CouponTemplateResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok(templateService.activate(id));
    }

    /** 暂停。 */
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<Void> pause(@PathVariable Long id) {
        templateService.pause(id);
        return ApiResponse.ok();
    }

    /** 恢复。 */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        templateService.resume(id);
        return ApiResponse.ok();
    }

    /** 终止。 */
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<Void> terminate(@PathVariable Long id) {
        templateService.terminate(id);
        return ApiResponse.ok();
    }

    /** 模板详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('coupon:manage')")
    public ApiResponse<CouponTemplateResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(templateService.get(id));
    }
}
