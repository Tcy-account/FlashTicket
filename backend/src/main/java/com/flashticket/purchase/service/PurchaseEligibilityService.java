package com.flashticket.purchase.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.SalePlan;
import com.flashticket.event.entity.Session;
import com.flashticket.event.entity.TicketTier;
import com.flashticket.event.entity.Venue;
import com.flashticket.event.enums.EventStatus;
import com.flashticket.event.enums.SaleStatus;
import com.flashticket.event.enums.SessionStatus;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.event.mapper.SalePlanMapper;
import com.flashticket.event.mapper.SessionMapper;
import com.flashticket.event.mapper.TicketTierMapper;
import com.flashticket.event.mapper.VenueMapper;
import com.flashticket.order.dto.OrderSnapshot;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 抢票资格校验服务（M12 提取复用）。
 *
 * <p>受理入口做「预检」（快速拒绝无效请求），消费端做「复检」（建单前再次确认
 * 业务条件，避免排队期间业务状态变化导致错误售票）。二者复用本服务的
 * validateAndSnapshot：校验活动已发布、场次正常、售票计划未暂停、处于售票窗口、
 * 数量合法，并构造成交快照（含 eventId/sessionId 供请求追踪）。
 */
@Service
public class PurchaseEligibilityService {

    private final TicketTierMapper tierMapper;
    private final SessionMapper sessionMapper;
    private final EventMapper eventMapper;
    private final SalePlanMapper salePlanMapper;
    private final VenueMapper venueMapper;

    public PurchaseEligibilityService(TicketTierMapper tierMapper,
                                      SessionMapper sessionMapper,
                                      EventMapper eventMapper,
                                      SalePlanMapper salePlanMapper,
                                      VenueMapper venueMapper) {
        this.tierMapper = tierMapper;
        this.sessionMapper = sessionMapper;
        this.eventMapper = eventMapper;
        this.salePlanMapper = salePlanMapper;
        this.venueMapper = venueMapper;
    }

    /**
     * 校验抢票资格并构造成交快照（见 doc/抢票请求与异步排队模块.md 5.1/5.2）。
     *
     * <p>此处校验的是「当前是否具备购票资格」，不保证最终一定能抢到
     * （余票为前置过滤，最终由库存条件扣减裁决）。
     */
    public OrderSnapshot validateAndSnapshot(Long tierId, int quantity) {
        TicketTier tier = tierMapper.selectById(tierId);
        if (tier == null) {
            throw new BizException(ErrorCode.EVENT_TIER_NOT_FOUND);
        }
        Session session = sessionMapper.selectById(tier.getSessionId());
        if (session == null) {
            throw new BizException(ErrorCode.EVENT_SESSION_NOT_FOUND);
        }
        Event event = eventMapper.selectById(session.getEventId());
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 活动已发布
        if (!EventStatus.PUBLISHED.name().equals(event.getStatus())) {
            throw new BizException(ErrorCode.PURCHASE_EVENT_NOT_PUBLISHED);
        }
        // 场次正常
        if (!SessionStatus.SCHEDULED.name().equals(session.getStatus())) {
            throw new BizException(ErrorCode.PURCHASE_SESSION_NOT_SALE);
        }
        // 售票计划已配置且未暂停
        SalePlan plan = salePlanMapper.findByTierId(tierId);
        if (plan == null) {
            throw new BizException(ErrorCode.EVENT_SALE_PLAN_NOT_FOUND);
        }
        if (SaleStatus.PAUSED.name().equals(plan.getSaleStatus())) {
            throw new BizException(ErrorCode.PURCHASE_SALE_PAUSED);
        }
        // 售票时间窗口（半开区间 [saleStartAt, saleEndAt)）
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (now.isBefore(plan.getSaleStartAt()) || !now.isBefore(plan.getSaleEndAt())) {
            throw new BizException(ErrorCode.PURCHASE_WINDOW_NOT_OPEN);
        }
        // 购买数量合法
        if (quantity < tier.getMinPurchase() || quantity > tier.getMaxPurchase()) {
            throw new BizException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }

        Venue venue = venueMapper.selectById(session.getVenueId());
        String venueName = venue == null ? "" : venue.getName();

        return new OrderSnapshot(
                event.getId(), event.getTitle(),
                session.getId(), session.getStartAt(), venueName,
                tier.getId(), tier.getName(), tier.getUnitPriceCent(), quantity);
    }
}
