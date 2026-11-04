package com.flashticket.event.enums;

/**
 * 活动审核记录状态（表 ft_event_review.review_status）。
 *
 * <p>见 doc/演出活动与场次.md 3.4 / 跨域审查 C05：提交审核生成配置指纹，
 * 审核通过后发布前仍保持配置冻结。
 */
public enum ReviewStatus {
    /** 待审核。 */
    PENDING,
    /** 审核通过。 */
    APPROVED,
    /** 审核驳回（须填写原因）。 */
    REJECTED
}
