package com.flashticket.order.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.order.dto.OrderOperationResponse;
import com.flashticket.order.dto.OrderResponse;
import com.flashticket.order.service.OrderCloseService;
import com.flashticket.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端订单查询与取消入口（M08 查询 + M09 取消/流水）。
 *
 * <p>仅登录用户可访问，且只能操作本人订单（归属校验，防止越权）。订单创建不由
 * 用户端直接发起，由抢票成功驱动（见 doc/订单与交易状态机模块.md 十一）。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderCloseService orderCloseService;

    public OrderController(OrderService orderService, OrderCloseService orderCloseService) {
        this.orderService = orderService;
        this.orderCloseService = orderCloseService;
    }

    /** 我的订单分页（可按状态筛选）。 */
    @GetMapping
    public ApiResponse<PageResult<OrderResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(orderService.listMyOrders(
                SecurityUtils.currentUserId(), status, page, pageSize));
    }

    /** 我的订单详情。 */
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getMyOrder(SecurityUtils.currentUserId(), orderId));
    }

    /** 取消待支付订单（重复取消幂等）。 */
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long orderId) {
        return ApiResponse.ok(orderCloseService.cancelByUser(SecurityUtils.currentUserId(), orderId));
    }

    /** 我的订单操作流水（状态变更历史）。 */
    @GetMapping("/{orderId}/operations")
    public ApiResponse<List<OrderOperationResponse>> operations(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.listOperations(SecurityUtils.currentUserId(), orderId));
    }
}
