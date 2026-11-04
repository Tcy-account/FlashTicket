package com.flashticket.analytics.dto;

import java.time.LocalDateTime;

/**
 * 指定活动的场次/票档销售明细（MyBatis 结果映射 POJO）。
 */
public class EventSalesDetailResponse {

    /** 场次 ID。 */
    private Long sessionId;

    /** 场次开始时间。 */
    private LocalDateTime sessionStartAt;

    /** 票档 ID。 */
    private Long tierId;

    /** 票档名称。 */
    private String tierName;

    /** 支付订单数。 */
    private Long orderCount;

    /** 售出票数。 */
    private Long ticketCount;

    /** 票面销售额（分）。 */
    private Long amountCent;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getSessionStartAt() {
        return sessionStartAt;
    }

    public void setSessionStartAt(LocalDateTime sessionStartAt) {
        this.sessionStartAt = sessionStartAt;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public Long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Long ticketCount) {
        this.ticketCount = ticketCount;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }
}
