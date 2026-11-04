package com.flashticket.event.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.SalePlanResponse;
import com.flashticket.event.dto.TicketTierRequest;
import com.flashticket.event.dto.TicketTierResponse;
import com.flashticket.event.dto.TierAvailabilityResponse;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.SalePlan;
import com.flashticket.event.entity.Session;
import com.flashticket.event.entity.TicketTier;
import com.flashticket.event.enums.EventStatus;
import com.flashticket.event.enums.SaleStatus;
import com.flashticket.event.enums.SessionStatus;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.event.mapper.SalePlanMapper;
import com.flashticket.event.mapper.SessionMapper;
import com.flashticket.event.mapper.TicketTierMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import com.flashticket.inventory.dto.InventoryResponse;
import com.flashticket.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 票档编排服务（M06）：创建、编辑、列表，及场次容量约束校验。
 *
 * <p>规则（见 doc/演出活动与场次.md 六）：
 * <ul>
 *   <li>票档名称同场次内唯一；</li>
 *   <li>同一场次所有票档初始库存总和不得超过场次可售容量；</li>
 *   <li>票价、限购等关键配置在活动进入审核/发布流程后冻结；</li>
 *   <li>库存扣减由库存中心 M07 负责，本模块不直接修改库存计数。</li>
 * </ul>
 */
@Service
public class TicketTierService {

    private static final String DEFAULT_TIER_TYPE = "GENERAL_ADMISSION";

    private final TicketTierMapper tierMapper;
    private final SalePlanMapper salePlanMapper;
    private final SessionMapper sessionMapper;
    private final EventMapper eventMapper;
    private final OperationAuditService auditService;
    private final InventoryService inventoryService;

    public TicketTierService(TicketTierMapper tierMapper,
                             SalePlanMapper salePlanMapper,
                             SessionMapper sessionMapper,
                             EventMapper eventMapper,
                             OperationAuditService auditService,
                             InventoryService inventoryService) {
        this.tierMapper = tierMapper;
        this.salePlanMapper = salePlanMapper;
        this.sessionMapper = sessionMapper;
        this.eventMapper = eventMapper;
        this.auditService = auditService;
        this.inventoryService = inventoryService;
    }

    /** 后台：某场次的全部票档（含售票计划）。 */
    public List<TicketTierResponse> listBySession(Long sessionId) {
        requireSession(sessionId);
        return buildResponses(tierMapper.findBySessionId(sessionId));
    }

    /** 创建票档。 */
    @Transactional
    public TicketTierResponse create(Long sessionId, TicketTierRequest req,
                                     AuthUser operator, String ip) {
        Session session = requireSession(sessionId);
        requireEditableEvent(session.getEventId());
        assertNameAvailable(sessionId, req.name().trim(), null);
        assertCapacity(session, req.initialQuantity(), null);

        TicketTier tier = new TicketTier();
        tier.setSessionId(sessionId);
        applyRequest(tier, req);
        tier.setVersion(0L);
        tierMapper.insert(tier);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.TIER_CREATE,
                OperationAction.TARGET_TIER, tier.getId(),
                Map.of("sessionId", sessionId, "name", tier.getName(),
                        "unitPriceCent", tier.getUnitPriceCent(), "initialQuantity", tier.getInitialQuantity()), ip);

        return toResponse(tier, null);
    }

    /** 编辑票档（乐观锁版本校验）。 */
    @Transactional
    public TicketTierResponse update(Long tierId, TicketTierRequest req,
                                     AuthUser operator, String ip) {
        TicketTier tier = requireTier(tierId);
        Session session = requireSession(tier.getSessionId());
        requireEditableEvent(session.getEventId());
        assertVersion(tier, req.version());
        assertNameAvailable(tier.getSessionId(), req.name().trim(), tierId);
        assertCapacity(session, req.initialQuantity(), tier);

        applyRequest(tier, req);
        updateWithVersionCheck(tier);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.TIER_UPDATE,
                OperationAction.TARGET_TIER, tierId,
                Map.of("name", tier.getName(), "unitPriceCent", tier.getUnitPriceCent(),
                        "initialQuantity", tier.getInitialQuantity()), ip);

        SalePlan plan = salePlanMapper.findByTierId(tierId);
        return toResponse(tier, plan);
    }

    /** 公开：某场次可展示票档（仅活动已发布且场次正常）。 */
    public List<TicketTierResponse> listPublishedBySession(Long sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || !SessionStatus.SCHEDULED.name().equals(session.getStatus())) {
            return List.of();
        }
        Event event = eventMapper.selectById(session.getEventId());
        if (event == null || !EventStatus.PUBLISHED.name().equals(event.getStatus())) {
            return List.of();
        }
        return buildResponses(tierMapper.findBySessionId(sessionId));
    }

    /**
     * 公开：票档可售状态（三层销售状态，见 doc/演出活动与场次.md 7.2）。
     *
     * <p>saleStatus 为运营手动控制，windowStatus 为时间窗口，eligible 为当前
     * 是否可发起抢票的资格结论（活动已发布 + 场次正常 + 未暂停 + 在窗口内）。
     */
    public TierAvailabilityResponse getAvailability(Long tierId) {
        TicketTier tier = requireTier(tierId);
        Session session = requireSession(tier.getSessionId());
        Event event = eventMapper.selectById(session.getEventId());
        SalePlan plan = salePlanMapper.findByTierId(tierId);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String saleStatus = plan == null ? null : plan.getSaleStatus();
        String windowStatus = windowStatus(plan, now);

        boolean eventPublished = event != null && EventStatus.PUBLISHED.name().equals(event.getStatus());
        boolean sessionScheduled = SessionStatus.SCHEDULED.name().equals(session.getStatus());
        boolean saleEnabled = plan != null && SaleStatus.ENABLED.name().equals(plan.getSaleStatus());
        boolean eligible = eventPublished && sessionScheduled && saleEnabled
                && "IN_PROGRESS".equals(windowStatus);

        // 库存中心 M07 接入实时余量（未初始化时为 null）
        InventoryResponse inventory = inventoryService.getInventoryOrNull(tierId);

        return new TierAvailabilityResponse(
                String.valueOf(tier.getId()), String.valueOf(tier.getSessionId()),
                event == null ? null : String.valueOf(event.getId()),
                tier.getName(), tier.getUnitPriceCent(),
                saleStatus, windowStatus, eligible,
                plan == null ? null : plan.getSaleStartAt(),
                plan == null ? null : plan.getSaleEndAt(),
                now,
                inventory == null ? null : inventory.available(),
                inventory == null ? null : inventory.total());
    }

    /* ---------- 内部 ---------- */

    /** 时间窗口状态：半开区间 [saleStartAt, saleEndAt)。 */
    private String windowStatus(SalePlan plan, LocalDateTime now) {
        if (plan == null) {
            return null;
        }
        if (now.isBefore(plan.getSaleStartAt())) {
            return "NOT_STARTED";
        }
        if (!now.isBefore(plan.getSaleEndAt())) {
            return "ENDED";
        }
        return "IN_PROGRESS";
    }

    private void applyRequest(TicketTier tier, TicketTierRequest req) {
        tier.setName(req.name().trim());
        tier.setTierType(trimToNull(req.tierType()) == null ? DEFAULT_TIER_TYPE : req.tierType().trim());
        tier.setUnitPriceCent(req.unitPriceCent());
        tier.setInitialQuantity(req.initialQuantity());
        tier.setMinPurchase(req.minPurchase() == null ? 1 : req.minPurchase());
        tier.setMaxPurchase(req.maxPurchase() == null ? 2 : req.maxPurchase());
        tier.setPerUserLimit(req.perUserLimit() == null ? 2 : req.perUserLimit());
        tier.setDescription(trimToNull(req.description()));
        tier.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        if (tier.getMaxPurchase() < tier.getMinPurchase()) {
            throw new BizException(ErrorCode.EVENT_TIER_INVALID_LIMIT);
        }
    }

    private void assertNameAvailable(Long sessionId, String name, Long excludeTierId) {
        boolean duplicated = tierMapper.findBySessionId(sessionId).stream()
                .anyMatch(t -> t.getName().equals(name) && !Objects.equals(t.getId(), excludeTierId));
        if (duplicated) {
            throw new BizException(ErrorCode.EVENT_TIER_NAME_TAKEN);
        }
    }

    /** 容量约束：该场次所有票档初始库存总和不得超过场次可售容量。 */
    private void assertCapacity(Session session, Integer newQuantity, TicketTier currentTier) {
        long sum = tierMapper.sumInitialQuantityBySessionId(session.getId());
        if (currentTier != null) {
            sum -= currentTier.getInitialQuantity();
        }
        if (sum + newQuantity > session.getSaleCapacity()) {
            throw new BizException(ErrorCode.EVENT_TIER_CAPACITY_EXCEED);
        }
    }

    private Session requireSession(Long sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.EVENT_SESSION_NOT_FOUND);
        }
        return session;
    }

    private TicketTier requireTier(Long tierId) {
        TicketTier tier = tierMapper.selectById(tierId);
        if (tier == null) {
            throw new BizException(ErrorCode.EVENT_TIER_NOT_FOUND);
        }
        return tier;
    }

    /** 活动配置冻结：仅草稿/审核驳回状态可编辑关键配置（见跨域审查 C05）。 */
    private void requireEditableEvent(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        String status = event.getStatus();
        if (!EventStatus.DRAFT.name().equals(status) && !EventStatus.REJECTED.name().equals(status)) {
            throw new BizException(ErrorCode.EVENT_CONFIG_FROZEN);
        }
    }

    private void assertVersion(TicketTier tier, Long requestVersion) {
        if (requestVersion == null || !Objects.equals(tier.getVersion(), requestVersion)) {
            throw new BizException(ErrorCode.EVENT_TIER_VERSION_CONFLICT);
        }
    }

    private void updateWithVersionCheck(TicketTier tier) {
        int rows = tierMapper.updateById(tier);
        if (rows == 0) {
            throw new BizException(ErrorCode.EVENT_TIER_VERSION_CONFLICT);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<TicketTierResponse> buildResponses(List<TicketTier> tiers) {
        if (tiers.isEmpty()) {
            return List.of();
        }
        List<Long> tierIds = tiers.stream().map(TicketTier::getId).toList();
        Map<Long, SalePlan> planByTier = salePlanMapper.findByTierIds(tierIds).stream()
                .collect(Collectors.toMap(SalePlan::getTierId, Function.identity()));
        return tiers.stream()
                .map(t -> toResponse(t, planByTier.get(t.getId())))
                .toList();
    }

    private static TicketTierResponse toResponse(TicketTier t, SalePlan plan) {
        return new TicketTierResponse(
                String.valueOf(t.getId()), String.valueOf(t.getSessionId()), t.getName(), t.getTierType(),
                t.getUnitPriceCent(), t.getInitialQuantity(), t.getMinPurchase(), t.getMaxPurchase(),
                t.getPerUserLimit(), t.getDescription(), t.getSortOrder(), t.getVersion(),
                plan == null ? null : toSalePlanResponse(plan),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    private static SalePlanResponse toSalePlanResponse(SalePlan p) {
        return new SalePlanResponse(
                String.valueOf(p.getId()), String.valueOf(p.getTierId()), p.getSaleStartAt(), p.getSaleEndAt(),
                p.getSaleStatus(), p.getSaleVersion(), p.getPausedAt(), p.getPauseReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
