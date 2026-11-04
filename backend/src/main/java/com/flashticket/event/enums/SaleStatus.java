package com.flashticket.event.enums;

/**
 * 售票计划运营控制状态（表 ft_sale_plan.sale_status）。
 *
 * <p>见 doc/演出活动与场次.md 7.2：销售状态分三层计算——
 * 第一层运营手动控制（ENABLED/PAUSED）、第二层时间窗口（NOT_STARTED/
 * IN_PROGRESS/ENDED）、第三层库存展示（AVAILABLE/SOLD_OUT）。
 * 本枚举只承载第一层运营控制；最终可售性由时间窗口与库存共同推导。
 */
public enum SaleStatus {
    /** 允许按计划销售。 */
    ENABLED,
    /** 运营暂停销售，禁止新抢票。 */
    PAUSED
}
