package com.flashticket.analytics.dto;

/**
 * 近 N 日实收趋势点（MyBatis 结果映射 POJO）。
 */
public class DailyTrendPoint {

    /** 日期（yyyy-MM-dd）。 */
    private String day;

    /** 当日实收金额（分）。 */
    private Long amountCent;

    /** 当日支付订单数。 */
    private Long orderCount;

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }
}
