package com.flashticket.event.enums;

/**
 * 演出分类状态（表 ft_event_category.status）。
 */
public enum CategoryStatus {
    /** 启用，前台可见、可被活动引用。 */
    ENABLED,
    /** 停用，前台不可见；已引用活动不受影响。 */
    DISABLED
}
