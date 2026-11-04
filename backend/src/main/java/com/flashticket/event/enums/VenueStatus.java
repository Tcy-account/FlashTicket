package com.flashticket.event.enums;

/**
 * 演出场馆状态（表 ft_venue.status）。
 */
public enum VenueStatus {
    /** 启用，可被场次引用。 */
    ENABLED,
    /** 停用，不可再被新场次引用；已有场次不受影响。 */
    DISABLED
}
