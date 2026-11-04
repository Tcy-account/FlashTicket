package com.flashticket.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审核活动请求（见 doc/演出活动与场次.md 3.4）。
 *
 * <p>approve=true 表示审核通过（活动进入 APPROVED 待发布）；false 表示驳回
 * （活动回到 REJECTED），驳回时必须填写原因 comment。
 */
public record ReviewRequest(
        @NotNull(message = "审核结论不能为空")
        Boolean approve,

        @Size(max = 500, message = "审核意见最长 500 字符")
        String comment) {
}
