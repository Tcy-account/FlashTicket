package com.flashticket.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 创建/编辑演出场次请求（活动 ID 从路径传入，不入请求体）。
 *
 * <p>时间均以 UTC 存储；end_at 必须晚于 start_at、entry_start_at 必须早于
 * start_at、sale_capacity 不得超过场馆核定容量等规则在服务层校验。
 * version 用于乐观并发控制（编辑时回传）。
 */
public record SessionRequest(
        @NotBlank(message = "场馆不能为空")
        @Pattern(regexp = "\\d+", message = "场馆 ID 格式非法")
        String venueId,

        @NotBlank(message = "场次名称不能为空")
        @Size(max = 64, message = "场次名称最长 64 字符")
        String name,

        @NotNull(message = "演出开始时间不能为空")
        LocalDateTime startAt,

        @NotNull(message = "预计结束时间不能为空")
        LocalDateTime endAt,

        LocalDateTime entryStartAt,

        @NotNull(message = "可售容量不能为空")
        @Min(value = 0, message = "可售容量不能为负")
        Integer saleCapacity,

        Long version) {
}
