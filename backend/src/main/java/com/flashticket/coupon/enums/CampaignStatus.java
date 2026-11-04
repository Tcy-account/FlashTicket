package com.flashticket.coupon.enums;

/**
 * 营销活动状态机（表 ft_campaign.status）。
 * <pre>
 * DRAFT → PENDING_REVIEW → PUBLISHED → ENDED
 *                              └──────────→ CANCELLED
 * 审核驳回：PENDING_REVIEW → DRAFT
 * </pre>
 */
public enum CampaignStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    ENDED,
    CANCELLED
}
