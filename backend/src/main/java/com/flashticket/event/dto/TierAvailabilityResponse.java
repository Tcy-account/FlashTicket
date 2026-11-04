package com.flashticket.event.dto;

import java.time.LocalDateTime;

/**
 * 票档公开可售状态视图（见 doc/演出活动与场次.md 7.2 三层销售状态）。
 *
 * <p>saleStatus 为运营手动控制（ENABLED/PAUSED）；windowStatus 为时间窗口
 * （NOT_STARTED/IN_PROGRESS/ENDED）；eligible 为当前是否可发起抢票的资格
 * 结论（活动已发布 + 场次正常 + 运营未暂停 + 处于售票窗口）。availableQuantity
 * 与 totalQuantity 为库存中心 M07 的实时余量，票档未初始化库存时为 null。
 */
public record TierAvailabilityResponse(
        String tierId,
        String sessionId,
        String eventId,
        String tierName,
        Long unitPriceCent,
        String saleStatus,
        String windowStatus,
        boolean eligible,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt,
        LocalDateTime serverNow,
        Integer availableQuantity,
        Integer totalQuantity) {
}
