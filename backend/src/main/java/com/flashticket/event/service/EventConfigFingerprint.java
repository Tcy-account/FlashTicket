package com.flashticket.event.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.SalePlan;
import com.flashticket.event.entity.Session;
import com.flashticket.event.entity.TicketTier;
import com.flashticket.event.entity.Venue;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.event.mapper.SalePlanMapper;
import com.flashticket.event.mapper.SessionMapper;
import com.flashticket.event.mapper.TicketTierMapper;
import com.flashticket.event.mapper.VenueMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动审核配置指纹（跨域审查 C05）。
 *
 * <p>采用整体审核配置指纹而非仅检查 Event 行版本：活动主表版本不变时，
 * 运营可能已改动某票档售价。本组件将审核范围内的配置规范化后计算 SHA-256
 * 摘要，覆盖活动信息、有效场次、场馆安排、票档价格/数量/限购、售票计划
 * 与销售控制。提交审核时生成，发布时重新计算并比对，防止「审核通过后被
 * 非法修改仍可发布」。
 */
@Component
public class EventConfigFingerprint {

    private final EventMapper eventMapper;
    private final SessionMapper sessionMapper;
    private final VenueMapper venueMapper;
    private final TicketTierMapper tierMapper;
    private final SalePlanMapper salePlanMapper;

    public EventConfigFingerprint(EventMapper eventMapper,
                                  SessionMapper sessionMapper,
                                  VenueMapper venueMapper,
                                  TicketTierMapper tierMapper,
                                  SalePlanMapper salePlanMapper) {
        this.eventMapper = eventMapper;
        this.sessionMapper = sessionMapper;
        this.venueMapper = venueMapper;
        this.tierMapper = tierMapper;
        this.salePlanMapper = salePlanMapper;
    }

    /** 计算指定活动的审核配置 SHA-256 摘要（hex，64 字符）。 */
    public String compute(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("schema=1\n");
        appendEvent(sb, event);

        List<Session> sessions = sessionMapper.findByEventId(eventId);
        sessions.sort(Comparator.comparing(Session::getId));
        List<Long> sessionIds = new ArrayList<>();
        for (Session session : sessions) {
            appendSession(sb, session);
            Venue venue = venueMapper.selectById(session.getVenueId());
            appendVenue(sb, venue);
            sessionIds.add(session.getId());
        }

        // 票档与售票计划按 ID 升序，保证顺序确定
        List<TicketTier> tiers = new ArrayList<>();
        for (Long sessionId : sessionIds) {
            tiers.addAll(tierMapper.findBySessionId(sessionId));
        }
        tiers.sort(Comparator.comparing(TicketTier::getId));

        Map<Long, SalePlan> planByTier = new HashMap<>();
        if (!tiers.isEmpty()) {
            List<Long> tierIds = tiers.stream().map(TicketTier::getId).toList();
            for (SalePlan plan : salePlanMapper.findByTierIds(tierIds)) {
                planByTier.put(plan.getTierId(), plan);
            }
        }

        for (TicketTier tier : tiers) {
            appendTier(sb, tier);
            SalePlan plan = planByTier.get(tier.getId());
            if (plan != null) {
                appendSalePlan(sb, plan);
            }
        }

        return sha256(sb.toString());
    }

    private void appendEvent(StringBuilder sb, Event e) {
        sb.append("event|").append(e.getId())
                .append('|').append(nvl(e.getCategoryId()))
                .append('|').append(nvl(e.getTitle()))
                .append('|').append(nvl(e.getSubtitle()))
                .append('|').append(nvl(e.getCoverUrl()))
                .append('|').append(nvl(e.getDescription()))
                .append('|').append(nvl(e.getDetailContent()))
                .append('|').append(nvl(e.getOrganizerName()))
                .append('|').append(nvl(e.getAttendanceNotice()))
                .append('|').append(nvl(e.getRefundPolicy()))
                .append('\n');
    }

    private void appendSession(StringBuilder sb, Session s) {
        sb.append("session|").append(s.getId())
                .append('|').append(nvl(s.getVenueId()))
                .append('|').append(nvl(s.getName()))
                .append('|').append(nvl(s.getStartAt()))
                .append('|').append(nvl(s.getEndAt()))
                .append('|').append(nvl(s.getEntryStartAt()))
                .append('|').append(nvl(s.getSaleCapacity()))
                .append('|').append(nvl(s.getStatus()))
                .append('\n');
    }

    private void appendVenue(StringBuilder sb, Venue v) {
        if (v == null) {
            return;
        }
        sb.append("venue|").append(v.getId())
                .append('|').append(nvl(v.getName()))
                .append('|').append(nvl(v.getCity()))
                .append('|').append(nvl(v.getAddress()))
                .append('|').append(nvl(v.getTimezone()))
                .append('|').append(nvl(v.getMaxCapacity()))
                .append('\n');
    }

    private void appendTier(StringBuilder sb, TicketTier t) {
        sb.append("tier|").append(t.getId())
                .append('|').append(nvl(t.getSessionId()))
                .append('|').append(nvl(t.getName()))
                .append('|').append(nvl(t.getTierType()))
                .append('|').append(nvl(t.getUnitPriceCent()))
                .append('|').append(nvl(t.getInitialQuantity()))
                .append('|').append(nvl(t.getMinPurchase()))
                .append('|').append(nvl(t.getMaxPurchase()))
                .append('|').append(nvl(t.getPerUserLimit()))
                .append('|').append(nvl(t.getSortOrder()))
                .append('\n');
    }

    private void appendSalePlan(StringBuilder sb, SalePlan p) {
        sb.append("saleplan|").append(p.getTierId())
                .append('|').append(nvl(p.getSaleStartAt()))
                .append('|').append(nvl(p.getSaleEndAt()))
                .append('|').append(nvl(p.getSaleStatus()))
                .append('\n');
    }

    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
