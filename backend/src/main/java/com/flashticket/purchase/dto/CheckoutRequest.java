package com.flashticket.purchase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 同步抢票请求体（POST /api/v1/purchases）。
 *
 * <p>requestId 由客户端生成（UUID），作为抢票幂等键：网络重试/重复点击携带同一
 * requestId 不会重复下单（见 doc/抢票请求与异步排队模块.md 十二）。tierId 为雪花
 * 主键，String 传输避免 JS 精度丢失。
 */
public record CheckoutRequest(
        @NotBlank @Size(max = 64) String requestId,
        @NotBlank @Pattern(regexp = "\\d+") String tierId,
        @NotNull @Min(1) @Max(20) Integer quantity,
        String selectedCouponId) {
}
