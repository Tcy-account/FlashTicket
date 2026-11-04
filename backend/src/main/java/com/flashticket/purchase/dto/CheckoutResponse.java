package com.flashticket.purchase.dto;

import com.flashticket.order.dto.OrderResponse;

/**
 * 抢票受理结果（POST /api/v1/purchases 响应）。
 *
 * <p>M12 起异步排队：受理成功返回 QUEUED 与 requestId（HTTP 202），order 为 null；
 * 携带同一 requestId 重试且已建单时幂等返回 SUCCEEDED 与订单。前端不得把 QUEUED
 * 展示为抢票成功（见 doc/抢票请求与异步排队模块.md 一）。
 */
public record CheckoutResponse(
        String requestId,
        String status,
        OrderResponse order) {
}
