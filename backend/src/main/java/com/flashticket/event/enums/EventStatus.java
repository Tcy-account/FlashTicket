package com.flashticket.event.enums;

/**
 * 演出活动生命周期（表 ft_event.status）。
 *
 * <p>见 doc/演出活动与场次.md 3.3 / doc/全局逻辑ER模型.md 三：
 * 活动状态不等同于售票状态；是否可售由场次、售票计划、库存共同推导。
 * M05 启用 DRAFT/CANCELLED；M06 启用审核发布流（PENDING_REVIEW/REJECTED/
 * APPROVED/PUBLISHED）。ARCHIVED 留待演出完成后的归档流程。
 *
 * <p>审核发布流：DRAFT → PENDING_REVIEW → APPROVED → PUBLISHED；
 * 审核驳回 PENDING_REVIEW → REJECTED，可修改后重新提交。
 * APPROVED（审核通过待发布）单独成态，以满足跨域审查 C05「审核通过至
 * 发布之前也保持配置冻结」的边界。
 */
public enum EventStatus {
    /** 草稿，运营可编辑。 */
    DRAFT,
    /** 待审核。 */
    PENDING_REVIEW,
    /** 审核驳回，可修改后重新提交。 */
    REJECTED,
    /** 审核通过，待正式发布（配置仍冻结）。 */
    APPROVED,
    /** 审核通过并已发布，公开展示。 */
    PUBLISHED,
    /** 整场取消，触发订单后续处理。 */
    CANCELLED,
    /** 所有场次终结后的历史归档。 */
    ARCHIVED
}
