package com.flashticket.analytics.api;

import com.flashticket.analytics.dto.EventSalesDetailResponse;
import com.flashticket.analytics.dto.EventSalesResponse;
import com.flashticket.analytics.dto.OrderTrendPoint;
import com.flashticket.analytics.dto.PlatformOverviewResponse;
import com.flashticket.analytics.dto.PurchaseConversionResponse;
import com.flashticket.analytics.service.AnalyticsService;
import com.flashticket.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营统计后台入口（M16-A）。
 *
 * <p>所有统计查询实时 SQL 聚合，需 analytics:read 权限；
 * 金额均标注为「模拟数据」，口径见各接口注释。
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** 平台运营概览 + 近 N 日实收趋势（默认 7 天，上限 90 天）。 */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ApiResponse<PlatformOverviewResponse> overview(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(analyticsService.overview(days));
    }

    /** 演出销售排行。 */
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ApiResponse<List<EventSalesResponse>> events() {
        return ApiResponse.ok(analyticsService.events());
    }

    /** 指定活动的场次/票档销售明细。 */
    @GetMapping("/events/{id}")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ApiResponse<List<EventSalesDetailResponse>> eventDetail(@PathVariable("id") Long eventId) {
        return ApiResponse.ok(analyticsService.eventDetail(eventId));
    }

    /** 抢票链路转化统计。 */
    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ApiResponse<PurchaseConversionResponse> purchases() {
        return ApiResponse.ok(analyticsService.purchases());
    }

    /** 订单趋势（按天，默认 30 天，上限 90 天）。 */
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ApiResponse<List<OrderTrendPoint>> orders(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.orders(days));
    }
}
