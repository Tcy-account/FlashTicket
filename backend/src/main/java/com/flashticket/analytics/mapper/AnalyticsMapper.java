package com.flashticket.analytics.mapper;

import com.flashticket.analytics.dto.DailyTrendPoint;
import com.flashticket.analytics.dto.EventSalesDetailResponse;
import com.flashticket.analytics.dto.EventSalesResponse;
import com.flashticket.analytics.dto.OrderTrendPoint;
import com.flashticket.analytics.dto.PlatformOverviewResponse;
import com.flashticket.analytics.dto.PurchaseConversionResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营统计聚合查询（M16-A，实时 SQL 聚合，不落汇总表）。
 *
 * <p>所有金额 SUM 显式 CAST AS SIGNED，避免 MySQL SUM(BIGINT)→DECIMAL 无法映射到 Long。
 * 统计口径见 doc/运营数据统计与系统对账模块.md 二、四节。
 */
@Mapper
public interface AnalyticsMapper {

    /**
     * 平台概览聚合（前六个指标，比率/净收入由 Service 派生）。
     * 口径：实收=PAID 订单 paid_amount_cent 合计；票数=PAID 订单明细 quantity 合计；
     * 退款=SUCCEEDED 退款单 amount_cent 合计。
     */
    @Select("""
            SELECT
              (SELECT CAST(COALESCE(SUM(paid_amount_cent),0) AS SIGNED)
                 FROM ft_order WHERE status = 'PAID') AS paid_amount_cent,
              (SELECT COUNT(*) FROM ft_order WHERE status = 'PAID') AS paid_order_count,
              (SELECT CAST(COALESCE(SUM(oi.quantity),0) AS SIGNED)
                 FROM ft_order_item oi
                 JOIN ft_order o ON o.id = oi.order_id
                WHERE o.status = 'PAID') AS sold_ticket_count,
              (SELECT CAST(COALESCE(SUM(amount_cent),0) AS SIGNED)
                 FROM ft_refund WHERE status = 'SUCCEEDED') AS refund_amount_cent,
              (SELECT COUNT(DISTINCT order_id)
                 FROM ft_refund WHERE status = 'SUCCEEDED') AS refunded_order_count,
              (SELECT COUNT(*) FROM ft_order) AS order_count
            """)
    PlatformOverviewResponse selectOverviewAggregate();

    /** 近 N 日实收趋势（按支付日期聚合）。 */
    @Select("""
            SELECT DATE(paid_at) AS day,
                   CAST(COALESCE(SUM(paid_amount_cent),0) AS SIGNED) AS amount_cent,
                   COUNT(*) AS order_count
              FROM ft_order
             WHERE status = 'PAID' AND paid_at >= #{from}
             GROUP BY DATE(paid_at)
             ORDER BY day ASC
            """)
    List<DailyTrendPoint> selectDailyTrend(@Param("from") LocalDateTime from);

    /** 演出销售排行（按已支付订单明细聚合，金额为票面金额不含优惠券）。 */
    @Select("""
            SELECT oi.event_id AS event_id,
                   MAX(oi.event_title) AS event_title,
                   COUNT(DISTINCT o.id) AS order_count,
                   CAST(COALESCE(SUM(oi.quantity),0) AS SIGNED) AS ticket_count,
                   CAST(COALESCE(SUM(oi.amount_cent),0) AS SIGNED) AS amount_cent
              FROM ft_order_item oi
              JOIN ft_order o ON o.id = oi.order_id AND o.status = 'PAID'
             GROUP BY oi.event_id
             ORDER BY amount_cent DESC
            """)
    List<EventSalesResponse> selectEventSales();

    /** 指定活动的场次/票档销售明细。 */
    @Select("""
            SELECT oi.session_id AS session_id,
                   MAX(oi.session_start_at) AS session_start_at,
                   oi.tier_id AS tier_id,
                   MAX(oi.tier_name) AS tier_name,
                   COUNT(DISTINCT o.id) AS order_count,
                   CAST(COALESCE(SUM(oi.quantity),0) AS SIGNED) AS ticket_count,
                   CAST(COALESCE(SUM(oi.amount_cent),0) AS SIGNED) AS amount_cent
              FROM ft_order_item oi
              JOIN ft_order o ON o.id = oi.order_id AND o.status = 'PAID'
             WHERE oi.event_id = #{eventId}
             GROUP BY oi.session_id, oi.tier_id
             ORDER BY session_start_at ASC, tier_id ASC
            """)
    List<EventSalesDetailResponse> selectEventDetail(@Param("eventId") Long eventId);

    /** 抢票转化聚合（受理/成功/失败/处理中，成功率由 Service 派生）。 */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM ft_purchase_request) AS accepted_count,
              (SELECT COUNT(*) FROM ft_purchase_request WHERE status = 'SUCCEEDED') AS succeeded_count,
              (SELECT COUNT(*) FROM ft_purchase_request WHERE status = 'FAILED') AS failed_count,
              (SELECT COUNT(*) FROM ft_purchase_request
                WHERE status IN ('QUEUED','PROCESSING','RECOVERING')) AS processing_count
            """)
    PurchaseConversionResponse selectPurchaseAggregate();

    /** 订单创建/支付趋势（按创建日期聚合）。 */
    @Select("""
            SELECT DATE(created_at) AS day,
                   COUNT(*) AS created_count,
                   COUNT(IF(status = 'PAID', 1, NULL)) AS paid_count,
                   CAST(COALESCE(SUM(IF(status = 'PAID', paid_amount_cent, 0)),0) AS SIGNED)
                     AS paid_amount_cent
              FROM ft_order
             WHERE created_at >= #{from}
             GROUP BY DATE(created_at)
             ORDER BY day ASC
            """)
    List<OrderTrendPoint> selectOrderCreatedTrend(@Param("from") LocalDateTime from);

    /** 退款趋势（按成功日期聚合，仅填 day + refundAmountCent）。 */
    @Select("""
            SELECT DATE(succeeded_at) AS day,
                   CAST(COALESCE(SUM(amount_cent),0) AS SIGNED) AS refund_amount_cent
              FROM ft_refund
             WHERE status = 'SUCCEEDED' AND succeeded_at >= #{from}
             GROUP BY DATE(succeeded_at)
             ORDER BY day ASC
            """)
    List<OrderTrendPoint> selectRefundTrend(@Param("from") LocalDateTime from);
}
