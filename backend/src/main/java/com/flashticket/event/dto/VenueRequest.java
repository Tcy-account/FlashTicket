package com.flashticket.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建/编辑场馆请求。
 *
 * <p>容量规则（见 doc/演出活动与场次.md 4.1）：max_capacity > 0；
 * 同一场次票量总和不能超过场次可售容量，场次可售容量不能超过场馆核定容量。
 */
public record VenueRequest(
        @NotBlank(message = "场馆名称不能为空")
        @Size(max = 100, message = "场馆名称最长 100 字符")
        String name,

        @Size(max = 2, message = "国家代码最长 2 字符")
        String countryCode,

        @Size(max = 64, message = "省份最长 64 字符")
        String province,

        @NotBlank(message = "城市不能为空")
        @Size(max = 64, message = "城市最长 64 字符")
        String city,

        @Size(max = 64, message = "区县最长 64 字符")
        String district,

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 255, message = "详细地址最长 255 字符")
        String address,

        @Size(max = 64, message = "时区最长 64 字符")
        String timezone,

        @NotNull(message = "最大容量不能为空")
        @Min(value = 1, message = "最大容量必须大于 0")
        Integer maxCapacity,

        @Size(max = 1000, message = "场馆简介最长 1000 字符")
        String description) {
}
