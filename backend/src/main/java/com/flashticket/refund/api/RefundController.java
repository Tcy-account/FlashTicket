package com.flashticket.refund.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.refund.dto.RefundRequest;
import com.flashticket.refund.dto.RefundResponse;
import com.flashticket.refund.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端退款入口（M11）。
 *
 * <p>仅登录用户可访问，且只能申请/查询/撤销本人订单的退款（归属校验）。退款金额由
 * 服务端按原支付金额全额计算，用户不能通过请求篡改（见 doc/模拟支付与退款模块.md 九）。
 */
@RestController
@RequestMapping("/api/v1")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 申请退款（幂等：重复申请返回既有退款单）。 */
    @PostMapping("/orders/{orderId}/refunds")
    public ApiResponse<RefundResponse> apply(@PathVariable Long orderId,
                                             @Valid @RequestBody RefundRequest request) {
        return ApiResponse.ok(refundService.apply(
                SecurityUtils.currentUserId(), orderId, request));
    }

    /** 我的退款分页（可按状态筛选）。 */
    @GetMapping("/refunds")
    public ApiResponse<PageResult<RefundResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(refundService.listMine(
                SecurityUtils.currentUserId(), status, page, pageSize));
    }

    /** 查询退款进度（归属校验）。 */
    @GetMapping("/refunds/{refundId}")
    public ApiResponse<RefundResponse> detail(@PathVariable Long refundId) {
        return ApiResponse.ok(refundService.getMine(SecurityUtils.currentUserId(), refundId));
    }

    /** 撤销未审核的退款申请（归属校验，幂等）。 */
    @PostMapping("/refunds/{refundId}/cancel")
    public ApiResponse<RefundResponse> cancel(@PathVariable Long refundId) {
        return ApiResponse.ok(refundService.cancel(SecurityUtils.currentUserId(), refundId));
    }
}
