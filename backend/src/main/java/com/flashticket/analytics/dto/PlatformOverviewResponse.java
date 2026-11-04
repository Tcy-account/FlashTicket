package com.flashticket.analytics.dto;

import java.util.List;

/**
 * 平台运营概览（模拟支付口径，V1 直接 SQL 聚合实时计算）。
 *
 * <p>前六个指标由聚合 SQL 映射，比率与净收入由 Service 基于聚合值派生，
 * dailyTrend 为近 N 日实收趋势。
 */
public class PlatformOverviewResponse {

    /** 模拟实收金额（分）。 */
    private Long paidAmountCent;

    /** 已支付订单数。 */
    private Long paidOrderCount;

    /** 销售票数（已支付订单对应票数合计）。 */
    private Long soldTicketCount;

    /** 成功退款金额（分）。 */
    private Long refundAmountCent;

    /** 成功退款订单数。 */
    private Long refundedOrderCount;

    /** 订单创建总数。 */
    private Long orderCount;

    /** 订单支付率 = 已支付订单数 / 已创建订单数。 */
    private Double paymentRate;

    /** 退款率 = 成功退款订单数 / 已支付订单数。 */
    private Double refundRate;

    /** 净收入（分）= 实收金额 - 成功退款金额。 */
    private Long netRevenueCent;

    /** 近 N 日实收趋势。 */
    private List<DailyTrendPoint> dailyTrend;

    public Long getPaidAmountCent() {
        return paidAmountCent;
    }

    public void setPaidAmountCent(Long paidAmountCent) {
        this.paidAmountCent = paidAmountCent;
    }

    public Long getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(Long paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public Long getSoldTicketCount() {
        return soldTicketCount;
    }

    public void setSoldTicketCount(Long soldTicketCount) {
        this.soldTicketCount = soldTicketCount;
    }

    public Long getRefundAmountCent() {
        return refundAmountCent;
    }

    public void setRefundAmountCent(Long refundAmountCent) {
        this.refundAmountCent = refundAmountCent;
    }

    public Long getRefundedOrderCount() {
        return refundedOrderCount;
    }

    public void setRefundedOrderCount(Long refundedOrderCount) {
        this.refundedOrderCount = refundedOrderCount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public Double getPaymentRate() {
        return paymentRate;
    }

    public void setPaymentRate(Double paymentRate) {
        this.paymentRate = paymentRate;
    }

    public Double getRefundRate() {
        return refundRate;
    }

    public void setRefundRate(Double refundRate) {
        this.refundRate = refundRate;
    }

    public Long getNetRevenueCent() {
        return netRevenueCent;
    }

    public void setNetRevenueCent(Long netRevenueCent) {
        this.netRevenueCent = netRevenueCent;
    }

    public List<DailyTrendPoint> getDailyTrend() {
        return dailyTrend;
    }

    public void setDailyTrend(List<DailyTrendPoint> dailyTrend) {
        this.dailyTrend = dailyTrend;
    }
}
