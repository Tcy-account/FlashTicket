package com.flashticket.analytics.service;

import com.flashticket.analytics.dto.DailyTrendPoint;
import com.flashticket.analytics.dto.EventSalesDetailResponse;
import com.flashticket.analytics.dto.EventSalesResponse;
import com.flashticket.analytics.dto.OrderTrendPoint;
import com.flashticket.analytics.dto.PlatformOverviewResponse;
import com.flashticket.analytics.dto.PurchaseConversionResponse;
import com.flashticket.analytics.mapper.AnalyticsMapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营统计服务（M16-A）：实时 SQL 聚合，派生比率与趋势拼接。
 */
@Service
public class AnalyticsService {

    /** 统计窗口上限（天），防止超长范围查询拖垮数据库。 */
    private static final int MAX_DAYS = 90;

    private final AnalyticsMapper analyticsMapper;

    public AnalyticsService(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    /** 平台运营概览 + 近 N 日实收趋势。 */
    public PlatformOverviewResponse overview(int days) {
        validateDays(days);
        PlatformOverviewResponse agg = analyticsMapper.selectOverviewAggregate();
        long orderCount = agg.getOrderCount();
        long paidOrderCount = agg.getPaidOrderCount();
        agg.setPaymentRate(orderCount == 0 ? 0.0 : round2((double) paidOrderCount / orderCount));
        agg.setRefundRate(paidOrderCount == 0 ? 0.0
                : round2((double) agg.getRefundedOrderCount() / paidOrderCount));
        agg.setNetRevenueCent(agg.getPaidAmountCent() - agg.getRefundAmountCent());
        agg.setDailyTrend(analyticsMapper.selectDailyTrend(fromDaysAgo(days)));
        return agg;
    }

    /** 演出销售排行。 */
    public List<EventSalesResponse> events() {
        return analyticsMapper.selectEventSales();
    }

    /** 指定活动的场次/票档销售明细。 */
    public List<EventSalesDetailResponse> eventDetail(Long eventId) {
        return analyticsMapper.selectEventDetail(eventId);
    }

    /** 抢票链路转化统计。 */
    public PurchaseConversionResponse purchases() {
        PurchaseConversionResponse agg = analyticsMapper.selectPurchaseAggregate();
        long done = agg.getSucceededCount() + agg.getFailedCount();
        agg.setSuccessRate(done == 0 ? 0.0 : round2((double) agg.getSucceededCount() / done));
        return agg;
    }

    /** 订单趋势（按天：创建/支付/支付金额 + 退款金额）。 */
    public List<OrderTrendPoint> orders(int days) {
        validateDays(days);
        LocalDateTime from = fromDaysAgo(days);
        List<OrderTrendPoint> trend = analyticsMapper.selectOrderCreatedTrend(from);
        Map<String, Long> refundByDay = analyticsMapper.selectRefundTrend(from).stream()
                .collect(Collectors.toMap(OrderTrendPoint::getDay,
                        OrderTrendPoint::getRefundAmountCent, (a, b) -> a));
        for (OrderTrendPoint p : trend) {
            p.setRefundAmountCent(refundByDay.getOrDefault(p.getDay(), 0L));
        }
        return trend;
    }

    private void validateDays(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BizException(ErrorCode.ANALYTICS_RANGE_TOO_LARGE);
        }
    }

    private LocalDateTime fromDaysAgo(int days) {
        return LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
