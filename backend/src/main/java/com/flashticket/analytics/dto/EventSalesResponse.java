package com.flashticket.analytics.dto;

/**
 * 演出销售统计行（MyBatis 结果映射 POJO）。
 *
 * <p>amountCent 为已支付订单明细票面金额合计（不含优惠券抵扣），
 * 与平台概览的「实收金额」口径不同，展示时需区分。
 */
public class EventSalesResponse {

    /** 活动 ID。 */
    private Long eventId;

    /** 活动标题。 */
    private String eventTitle;

    /** 支付订单数。 */
    private Long orderCount;

    /** 售出票数。 */
    private Long ticketCount;

    /** 票面销售额（分，不含优惠券）。 */
    private Long amountCent;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
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
