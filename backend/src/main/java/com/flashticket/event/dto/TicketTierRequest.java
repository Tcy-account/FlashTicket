package com.flashticket.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建/编辑票档请求（场次 ID 从路径传入，不入请求体）。
 *
 * <p>票档管理「卖什么、卖多少、限购多少」（见 doc/演出活动与场次.md 六）：
 * 单价正整数分（支持 0 免费票）、初始总库存非负、每单/每用户限购规则。
 * version 用于乐观并发控制（编辑时回传）。
 */
public record TicketTierRequest(
        @NotBlank(message = "票档名称不能为空")
        @Size(max = 64, message = "票档名称最长 64 字符")
        String name,

        @Size(max = 24, message = "票档类型最长 24 字符")
        String tierType,

        @NotNull(message = "单价不能为空")
        @Min(value = 0, message = "单价不能为负")
        Long unitPriceCent,

        @NotNull(message = "初始库存不能为空")
        @Min(value = 0, message = "初始库存不能为负")
        Integer initialQuantity,

        @Min(value = 1, message = "每单最小购买数至少为 1")
        Integer minPurchase,

        @Min(value = 1, message = "每单最大购买数至少为 1")
        Integer maxPurchase,

        @Min(value = 1, message = "每用户限购数至少为 1")
        Integer perUserLimit,

        @Size(max = 500, message = "票档说明最长 500 字符")
        String description,

        Integer sortOrder,

        Long version) {
}
