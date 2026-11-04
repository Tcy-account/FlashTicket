package com.flashticket.coupon.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 营销活动审核请求（审核通过 / 驳回回草稿）。
 */
public record CampaignReviewRequest(
        @NotNull(message = "审核结论不能为空")
        Boolean approve,

        String comment) {
}
