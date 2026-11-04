package com.flashticket.event.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 配置售票计划请求（票档 ID 从路径传入）。
 *
 * <p>售票计划负责「何时卖」（见 doc/演出活动与场次.md 七）：开售/结束时间
 * 半开区间 [sale_start_at, sale_end_at)，结束时间必须晚于开始时间由服务层校验。
 * 每次时间变更递增 sale_version，供抢票/库存模块识别过期配置。
 */
public record SalePlanRequest(
        @NotNull(message = "开售时间不能为空")
        LocalDateTime saleStartAt,

        @NotNull(message = "售票结束时间不能为空")
        LocalDateTime saleEndAt) {
}
