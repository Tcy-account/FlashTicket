package com.flashticket.event.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.SalePlanRequest;
import com.flashticket.event.dto.SalePlanResponse;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.SalePlan;
import com.flashticket.event.entity.Session;
import com.flashticket.event.entity.TicketTier;
import com.flashticket.event.enums.EventStatus;
import com.flashticket.event.enums.SaleStatus;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.event.mapper.SalePlanMapper;
import com.flashticket.event.mapper.SessionMapper;
import com.flashticket.event.mapper.TicketTierMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 售票计划编排服务（M06）：配置开售窗口、暂停、恢复销售。
 *
 * <p>售票计划负责「何时卖、是否暂停」（见 doc/演出活动与场次.md 七），与抢票
 * 中心区分。sale_version 每次时间变更/暂停/恢复递增，供抢票/库存模块校验
 * 过期配置，避免 Redis 旧缓存错误放行。暂停/恢复属运营日常控制，允许在活动
 * 发布后执行；开售时间属关键配置，仅草稿/驳回状态可改（跨域审查 C05）。
 */
@Service
public class SalePlanService {

    private final SalePlanMapper salePlanMapper;
    private final TicketTierMapper tierMapper;
    private final SessionMapper sessionMapper;
    private final EventMapper eventMapper;
    private final OperationAuditService auditService;

    public SalePlanService(SalePlanMapper salePlanMapper,
                           TicketTierMapper tierMapper,
                           SessionMapper sessionMapper,
                           EventMapper eventMapper,
                           OperationAuditService auditService) {
        this.salePlanMapper = salePlanMapper;
        this.tierMapper = tierMapper;
        this.sessionMapper = sessionMapper;
        this.eventMapper = eventMapper;
        this.auditService = auditService;
    }

    /** 配置售票窗口（首次创建或更新时间，均递增 sale_version）。 */
    @Transactional
    public SalePlanResponse configure(Long tierId, SalePlanRequest req,
                                      AuthUser operator, String ip) {
        TicketTier tier = requireTier(tierId);
        requireEditableEvent(tier);
        if (!req.saleEndAt().isAfter(req.saleStartAt())) {
            throw new BizException(ErrorCode.EVENT_SALE_PLAN_INVALID_TIME);
        }

        SalePlan plan = salePlanMapper.findByTierId(tierId);
        if (plan == null) {
            plan = new SalePlan();
            plan.setTierId(tierId);
            plan.setSaleStartAt(req.saleStartAt());
            plan.setSaleEndAt(req.saleEndAt());
            plan.setSaleStatus(SaleStatus.ENABLED.name());
            plan.setSaleVersion(0L);
            salePlanMapper.insert(plan);
        } else {
            plan.setSaleStartAt(req.saleStartAt());
            plan.setSaleEndAt(req.saleEndAt());
            plan.setSaleVersion(plan.getSaleVersion() + 1);
            salePlanMapper.updateById(plan);
        }

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SALE_PLAN_UPDATE,
                OperationAction.TARGET_TIER, tierId,
                Map.of("saleStartAt", req.saleStartAt(), "saleEndAt", req.saleEndAt()), ip);

        return toResponse(plan);
    }

    /** 暂停销售：ENABLED → PAUSED（运营日常控制，可在发布后执行）。 */
    @Transactional
    public SalePlanResponse pause(Long tierId, String reason, AuthUser operator, String ip) {
        requireTier(tierId);
        SalePlan plan = requirePlan(tierId);
        if (!SaleStatus.ENABLED.name().equals(plan.getSaleStatus())) {
            throw new BizException(ErrorCode.EVENT_SALE_PLAN_ILLEGAL_TRANSITION);
        }

        plan.setSaleStatus(SaleStatus.PAUSED.name());
        plan.setPausedAt(LocalDateTime.now(ZoneOffset.UTC));
        plan.setPauseReason(reason.trim());
        plan.setSaleVersion(plan.getSaleVersion() + 1);
        salePlanMapper.updateById(plan);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SALE_PLAN_PAUSE,
                OperationAction.TARGET_TIER, tierId, Map.of("reason", plan.getPauseReason()), ip);

        return toResponse(plan);
    }

    /** 恢复销售：PAUSED → ENABLED。 */
    @Transactional
    public SalePlanResponse resume(Long tierId, AuthUser operator, String ip) {
        requireTier(tierId);
        SalePlan plan = requirePlan(tierId);
        if (!SaleStatus.PAUSED.name().equals(plan.getSaleStatus())) {
            throw new BizException(ErrorCode.EVENT_SALE_PLAN_ILLEGAL_TRANSITION);
        }

        plan.setSaleStatus(SaleStatus.ENABLED.name());
        plan.setPausedAt(null);
        plan.setPauseReason(null);
        plan.setSaleVersion(plan.getSaleVersion() + 1);
        salePlanMapper.updateById(plan);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SALE_PLAN_RESUME,
                OperationAction.TARGET_TIER, tierId, Map.of(), ip);

        return toResponse(plan);
    }

    /* ---------- 内部 ---------- */

    private TicketTier requireTier(Long tierId) {
        TicketTier tier = tierMapper.selectById(tierId);
        if (tier == null) {
            throw new BizException(ErrorCode.EVENT_TIER_NOT_FOUND);
        }
        return tier;
    }

    private SalePlan requirePlan(Long tierId) {
        SalePlan plan = salePlanMapper.findByTierId(tierId);
        if (plan == null) {
            throw new BizException(ErrorCode.EVENT_SALE_PLAN_NOT_FOUND);
        }
        return plan;
    }

    /** 开售时间属关键配置，仅草稿/审核驳回状态可配置（见跨域审查 C05）。 */
    private void requireEditableEvent(TicketTier tier) {
        Session session = sessionMapper.selectById(tier.getSessionId());
        if (session == null) {
            throw new BizException(ErrorCode.EVENT_SESSION_NOT_FOUND);
        }
        Event event = eventMapper.selectById(session.getEventId());
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        String status = event.getStatus();
        if (!EventStatus.DRAFT.name().equals(status) && !EventStatus.REJECTED.name().equals(status)) {
            throw new BizException(ErrorCode.EVENT_CONFIG_FROZEN);
        }
    }

    private static SalePlanResponse toResponse(SalePlan p) {
        return new SalePlanResponse(
                String.valueOf(p.getId()), String.valueOf(p.getTierId()), p.getSaleStartAt(), p.getSaleEndAt(),
                p.getSaleStatus(), p.getSaleVersion(), p.getPausedAt(), p.getPauseReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
