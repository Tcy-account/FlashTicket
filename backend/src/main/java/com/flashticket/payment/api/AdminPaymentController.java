package com.flashticket.payment.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.payment.dto.PaymentResponse;
import com.flashticket.payment.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台支付查询入口（M10），只读 payment:read 权限。
 *
 * <p>FINANCE / SUPER_ADMIN 可查看支付单（财务对账）。退款审核/重试等留待 M11。
 */
@RestController
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 支付单分页（状态/支付编号筛选）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('payment:read')")
    public ApiResponse<PageResult<PaymentResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentNo) {
        return ApiResponse.ok(paymentService.listAdmin(status, paymentNo, page, pageSize));
    }

    /** 支付单详情。 */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('payment:read')")
    public ApiResponse<PaymentResponse> detail(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.getAdmin(paymentId));
    }
}
