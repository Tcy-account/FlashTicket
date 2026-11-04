package com.flashticket.order.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.order.dto.OrderResponse;
import com.flashticket.order.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单查询入口（M08），只读 order:read 权限。
 *
 * <p>OPERATOR / FINANCE / SUPER_ADMIN 可查看交易订单（财务对账）。订单取消/人工
 * 关单（order:cancel/close）留待 M09 落地。
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 订单分页（状态/订单号/用户筛选）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public ApiResponse<PageResult<OrderResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.ok(orderService.listAdminOrders(status, orderNo, userId, page, pageSize));
    }

    /** 订单详情。 */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:read')")
    public ApiResponse<OrderResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrderForAdmin(orderId));
    }
}
