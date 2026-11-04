package com.flashticket.identity.domain;

/**
 * 后台敏感操作类型（表 ft_operation_audit.action），
 * 见 doc/用户与账号模块设计.md 十（审计日志与异常事件）。
 */
public final class OperationAction {

    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DISABLE = "USER_DISABLE";
    public static final String USER_ENABLE = "USER_ENABLE";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String SESSION_REVOKE = "SESSION_REVOKE";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ROLE_PERMISSION_GRANT = "ROLE_PERMISSION_GRANT";

    // ===== 演出域（M04 分类与场馆管理） =====
    public static final String CATEGORY_CREATE = "CATEGORY_CREATE";
    public static final String CATEGORY_UPDATE = "CATEGORY_UPDATE";
    public static final String CATEGORY_TOGGLE = "CATEGORY_TOGGLE";
    public static final String VENUE_CREATE = "VENUE_CREATE";
    public static final String VENUE_UPDATE = "VENUE_UPDATE";
    public static final String VENUE_TOGGLE = "VENUE_TOGGLE";

    // ===== 演出域（M05 活动与场次管理） =====
    public static final String EVENT_CREATE = "EVENT_CREATE";
    public static final String EVENT_UPDATE = "EVENT_UPDATE";
    public static final String EVENT_CANCEL = "EVENT_CANCEL";
    public static final String SESSION_CREATE = "SESSION_CREATE";
    public static final String SESSION_UPDATE = "SESSION_UPDATE";
    public static final String SESSION_POSTPONE = "SESSION_POSTPONE";
    public static final String SESSION_CANCEL = "SESSION_CANCEL";

    // ===== 演出域（M06 票档、售票计划与审核发布） =====
    public static final String TIER_CREATE = "TIER_CREATE";
    public static final String TIER_UPDATE = "TIER_UPDATE";
    public static final String SALE_PLAN_UPDATE = "SALE_PLAN_UPDATE";
    public static final String SALE_PLAN_PAUSE = "SALE_PLAN_PAUSE";
    public static final String SALE_PLAN_RESUME = "SALE_PLAN_RESUME";
    public static final String EVENT_SUBMIT_REVIEW = "EVENT_SUBMIT_REVIEW";
    public static final String EVENT_REVIEW = "EVENT_REVIEW";
    public static final String EVENT_PUBLISH = "EVENT_PUBLISH";

    /** 审计目标类型。 */
    public static final String TARGET_USER = "USER";
    public static final String TARGET_ROLE = "ROLE";
    public static final String TARGET_CATEGORY = "CATEGORY";
    public static final String TARGET_VENUE = "VENUE";
    public static final String TARGET_EVENT = "EVENT";
    public static final String TARGET_SESSION = "SESSION";
    public static final String TARGET_TIER = "TIER";

    private OperationAction() {
    }
}
