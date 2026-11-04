package com.flashticket.coupon.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.coupon.dto.CampaignRequest;
import com.flashticket.coupon.dto.CampaignResponse;
import com.flashticket.coupon.dto.CampaignReviewRequest;
import com.flashticket.coupon.service.CampaignService;
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
 * 后台营销活动管理入口（M13）。
 *
 * <p>活动创建/编辑/提交属 campaign:manage；审核属 campaign:review（SUPER_ADMIN）。
 */
@RestController
@RequestMapping("/api/v1/admin/campaigns")
public class AdminCampaignController {

    private final CampaignService campaignService;

    public AdminCampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /** 活动分页列表。 */
    @GetMapping
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<PageResult<CampaignResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(campaignService.list(page, pageSize, status));
    }

    /** 创建活动。 */
    @PostMapping
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<CampaignResponse> create(@Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok(campaignService.create(request, SecurityUtils.currentUser()));
    }

    /** 编辑活动（仅 DRAFT）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<CampaignResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok(campaignService.update(id, request));
    }

    /** 提交审核。 */
    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<Void> submitReview(@PathVariable Long id) {
        campaignService.submitReview(id);
        return ApiResponse.ok();
    }

    /** 审核（通过/驳回）。 */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('campaign:review')")
    public ApiResponse<Void> review(@PathVariable Long id,
                                    @Valid @RequestBody CampaignReviewRequest request) {
        campaignService.review(id, request, SecurityUtils.currentUser());
        return ApiResponse.ok();
    }

    /** 结束活动。 */
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<Void> end(@PathVariable Long id) {
        campaignService.endOrCancel(id, "ENDED");
        return ApiResponse.ok();
    }

    /** 取消活动。 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        campaignService.endOrCancel(id, "CANCELLED");
        return ApiResponse.ok();
    }

    /** 活动详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('campaign:manage')")
    public ApiResponse<CampaignResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(campaignService.get(id));
    }
}
