package com.flashticket.refund.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.refund.dto.RefundResponse;
import com.flashticket.refund.dto.ReviewRefundRequest;
import com.flashticket.refund.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台退款审核入口（M11）。
 *
 * <p>FINANCE / SUPER_ADMIN 可查看（refund:read）、审核通过/拒绝（refund:approve）、
 * 重试执行（refund:retry）。审核人不能审核自己提交的申请（服务层校验 REFUND_SELF_REVIEW）。
 */
@RestController
@RequestMapping("/api/v1/admin/refunds")
public class AdminRefundController {

    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 退款单分页（状态/退款号筛选）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('refund:read')")
    public ApiResponse<PageResult<RefundResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String refundNo) {
        return ApiResponse.ok(refundService.listAdmin(status, refundNo, page, pageSize));
    }

    /** 退款单详情。 */
    @GetMapping("/{refundId}")
    @PreAuthorize("hasAuthority('refund:read')")
    public ApiResponse<RefundResponse> detail(@PathVariable Long refundId) {
        return ApiResponse.ok(refundService.getAdmin(refundId));
    }

    /** 审核通过（并自动执行模拟退款到成功）。 */
    @PostMapping("/{refundId}/approve")
    @PreAuthorize("hasAuthority('refund:approve')")
    public ApiResponse<RefundResponse> approve(@PathVariable Long refundId,
                                               @Valid @RequestBody ReviewRefundRequest request) {
        return ApiResponse.ok(refundService.approve(
                SecurityUtils.currentUserId(), refundId, request.comment()));
    }

    /** 审核拒绝（必须填写原因）。 */
    @PostMapping("/{refundId}/reject")
    @PreAuthorize("hasAuthority('refund:approve')")
    public ApiResponse<RefundResponse> reject(@PathVariable Long refundId,
                                              @Valid @RequestBody ReviewRefundRequest request) {
        return ApiResponse.ok(refundService.reject(
                SecurityUtils.currentUserId(), refundId, request.comment()));
    }

    /** 重试退款执行（对 FAILED 的退款单复用业务单重新执行）。 */
    @PostMapping("/{refundId}/retry")
    @PreAuthorize("hasAuthority('refund:retry')")
    public ApiResponse<RefundResponse> retry(@PathVariable Long refundId) {
        return ApiResponse.ok(refundService.retry(SecurityUtils.currentUserId(), refundId));
    }
}
