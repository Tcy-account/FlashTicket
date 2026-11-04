package com.flashticket.event.enums;

/**
 * 演出场次状态（表 ft_session.status）。
 *
 * <p>见 doc/演出活动与场次.md 5.2：场次不设 SELLING/SOLD_OUT 冗余状态，
 * 是否可售由场次有效性、销售窗口、票档状态与库存共同判断。M05 启用
 * SCHEDULED/POSTPONED/CANCELLED，COMPLETED 留待后续演出完成流程。
 */
public enum SessionStatus {
    /** 正常场次。 */
    SCHEDULED,
    /** 已延期，暂停新销售。 */
    POSTPONED,
    /** 场次取消。 */
    CANCELLED,
    /** 演出完成。 */
    COMPLETED
}
