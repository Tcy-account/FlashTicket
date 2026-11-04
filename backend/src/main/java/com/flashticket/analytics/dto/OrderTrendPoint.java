package com.flashticket.analytics.dto;

/**
 * 订单趋势点（按天：创建数 / 支付数 / 支付金额 / 退款金额）。
 */
public class OrderTrendPoint {

    /** 日期（yyyy-MM-dd）。 */
    private String day;

    /** 当日创建订单数。 */
    private Long createdCount;

    /** 当日支付订单数。 */
    private Long paidCount;

    /** 当日支付金额（分）。 */
    private Long paidAmountCent;

    /** 当日退款金额（分）。 */
    private Long refundAmountCent;

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public Long getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Long createdCount) {
        this.createdCount = createdCount;
    }

    public Long getPaidCount() {
        return paidCount;
    }

    public void setPaidCount(Long paidCount) {
        this.paidCount = paidCount;
    }

    public Long getPaidAmountCent() {
        return paidAmountCent;
    }

    public void setPaidAmountCent(Long paidAmountCent) {
        this.paidAmountCent = paidAmountCent;
    }

    public Long getRefundAmountCent() {
        return refundAmountCent;
    }

    public void setRefundAmountCent(Long refundAmountCent) {
        this.refundAmountCent = refundAmountCent;
    }
}
