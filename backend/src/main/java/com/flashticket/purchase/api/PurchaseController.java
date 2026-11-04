package com.flashticket.purchase.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.purchase.dto.CheckoutRequest;
import com.flashticket.purchase.dto.CheckoutResponse;
import com.flashticket.purchase.dto.PurchaseStatusResponse;
import com.flashticket.purchase.service.PurchaseCheckoutService;
import com.flashticket.purchase.service.PurchaseQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端异步抢票入口（M12）。
 *
 * <p>提交抢票可靠受理后返回 HTTP 202 + QUEUED，前端据此跳转排队页轮询状态。
 * requestId 由客户端生成，重试携带同一 requestId 幂等返回既有状态。排队成功
 * 不代表抢票成功，只有 SUCCEEDED 才表示订单已创建。
 */
@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final PurchaseCheckoutService checkoutService;
    private final PurchaseQueryService queryService;

    public PurchaseController(PurchaseCheckoutService checkoutService,
                              PurchaseQueryService queryService) {
        this.checkoutService = checkoutService;
        this.queryService = queryService;
    }

    /** 提交抢票：可靠受理，返回 202 + QUEUED。 */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponse>> accept(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(checkoutService.accept(SecurityUtils.currentUserId(), request)));
    }

    /** 查询抢票状态（排队页轮询依据，归属校验）。 */
    @GetMapping("/{requestId}")
    public ApiResponse<PurchaseStatusResponse> status(@PathVariable String requestId) {
        return ApiResponse.ok(queryService.getStatus(SecurityUtils.currentUserId(), requestId));
    }

    /** 我的抢票历史分页。 */
    @GetMapping
    public ApiResponse<PageResult<PurchaseStatusResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(queryService.listMy(SecurityUtils.currentUserId(), page, pageSize));
    }

    /** 撤销尚未开始处理的排队请求（仅 QUEUED 可撤销，幂等）。 */
    @PostMapping("/{requestId}/cancel")
    public ApiResponse<PurchaseStatusResponse> cancel(@PathVariable String requestId) {
        return ApiResponse.ok(queryService.cancel(SecurityUtils.currentUserId(), requestId));
    }
}
