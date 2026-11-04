package com.flashticket.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 创建/编辑营销活动请求。
 */
public record CampaignRequest(
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 100, message = "活动名称最长 100 字符")
        String name,

        @NotBlank(message = "活动类型不能为空")
        String type,

        @Size(max = 500, message = "活动说明最长 500 字符")
        String description,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime startAt,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endAt) {
}
