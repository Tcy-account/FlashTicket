package com.flashticket.payment.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.payment.dto.MockPayRequest;
import com.flashticket.payment.dto.PaymentResponse;
import com.flashticket.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端模拟支付入口（M10）。
 *
 * <p>仅登录用户可访问，且只能操作本人订单的支付单（归属校验）。模拟支付必须由
 * 订单所有者触发，前端不能通过请求参数篡改金额或直接声明「支付成功」——金额由
 * 服务端核对订单应付金额，支付结果由内部模拟渠道产生（见 doc/模拟支付与退款模块.md 九）。
 */
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 创建或获取待支付订单的支付业务单。 */
    @PostMapping("/orders/{orderId}/payments")
    public ApiResponse<PaymentResponse> createOrGet(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.createOrGet(orderId, SecurityUtils.currentUserId()));
    }

    /** 查询支付单状态。 */
    @GetMapping("/payments/{paymentId}")
    public ApiResponse<PaymentResponse> detail(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.getMine(SecurityUtils.currentUserId(), paymentId));
    }

    /** 模拟执行支付（幂等：重复提交同一 attemptRequestId 不重复确认）。 */
    @PostMapping("/payments/{paymentId}/mock-pay")
    public ApiResponse<PaymentResponse> mockPay(@PathVariable Long paymentId,
                                                @Valid @RequestBody MockPayRequest request) {
        return ApiResponse.ok(paymentService.mockPay(
                paymentId, SecurityUtils.currentUserId(), request.attemptRequestId()));
    }
}
