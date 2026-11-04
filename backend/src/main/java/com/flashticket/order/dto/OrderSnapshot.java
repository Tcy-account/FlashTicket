package com.flashticket.order.dto;

import java.time.LocalDateTime;

/**
 * 订单创建快照（order 域内部 DTO，由抢票编排层构造后传入 OrderService）。
 *
 * <p>保存成交时的演出信息，避免运营后续改价/改名影响历史订单展示
 * （见 doc/订单与交易状态机模块.md 八、交易快照）。
 */
public record OrderSnapshot(
        Long eventId,
        String eventTitle,
        Long sessionId,
        LocalDateTime sessionStartAt,
        String venueName,
        Long tierId,
        String tierName,
        Long unitPriceCent,
        int quantity) {

    /** 本项金额 = 单价 × 数量。 */
    public long amountCent() {
        return unitPriceCent * quantity;
    }
}
