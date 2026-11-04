package com.flashticket.event.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.EventResponse;
import com.flashticket.event.dto.ReviewRequest;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.EventReview;
import com.flashticket.event.entity.SalePlan;
import com.flashticket.event.entity.Session;
import com.flashticket.event.entity.TicketTier;
import com.flashticket.event.enums.EventStatus;
import com.flashticket.event.enums.ReviewStatus;
import com.flashticket.event.enums.SessionStatus;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.event.mapper.EventReviewMapper;
import com.flashticket.event.mapper.SalePlanMapper;
import com.flashticket.event.mapper.SessionMapper;
import com.flashticket.event.mapper.TicketTierMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import com.flashticket.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * 活动审核发布编排服务（M06）：提交审核、审核（通过/驳回）、正式发布。
 *
 * <p>规则（见 doc/演出活动与场次.md 3.4 / 跨域审查 C05）：
 * <ul>
 *   <li>提交审核须完成活动、至少一个有效场次、票档与售票时间配置；</li>
 *   <li>审核人不能审核自己提交的活动；审核驳回必须填写原因；</li>
 *   <li>采用整体审核配置指纹 snapshot_hash：发布时重新校验配置与审核快照
 *       一致，防止「审核通过后被篡改仍可发布」；</li>
 *   <li>审核通过至发布前（APPROVED）配置保持冻结。</li>
 * </ul>
 */
@Service
public class EventReviewService {

    private final EventMapper eventMapper;
    private final SessionMapper sessionMapper;
    private final TicketTierMapper tierMapper;
    private final SalePlanMapper salePlanMapper;
    private final EventReviewMapper reviewMapper;
    private final EventConfigFingerprint fingerprint;
    private final OperationAuditService auditService;
    private final InventoryService inventoryService;

    public EventReviewService(EventMapper eventMapper,
                              SessionMapper sessionMapper,
                              TicketTierMapper tierMapper,
                              SalePlanMapper salePlanMapper,
                              EventReviewMapper reviewMapper,
                              EventConfigFingerprint fingerprint,
                              OperationAuditService auditService,
                              InventoryService inventoryService) {
        this.eventMapper = eventMapper;
        this.sessionMapper = sessionMapper;
        this.tierMapper = tierMapper;
        this.salePlanMapper = salePlanMapper;
        this.reviewMapper = reviewMapper;
        this.fingerprint = fingerprint;
        this.auditService = auditService;
        this.inventoryService = inventoryService;
    }

    /** 提交审核：DRAFT/REJECTED → PENDING_REVIEW，生成审核配置指纹。 */
    @Transactional
    public EventResponse submitReview(Long eventId, AuthUser operator, String ip) {
        Event event = requireEvent(eventId);
        String status = event.getStatus();
        if (!EventStatus.DRAFT.name().equals(status) && !EventStatus.REJECTED.name().equals(status)) {
            throw new BizException(ErrorCode.EVENT_SUBMIT_ILLEGAL_STATE);
        }
        assertSubmitReady(eventId);

        String hash = fingerprint.compute(eventId);
        EventReview review = new EventReview();
        review.setEventId(eventId);
        review.setSubmittedBy(operator.getUserId());
        review.setEventVersion(event.getVersion());
        review.setSnapshotHash(hash);
        review.setSnapshotSchemaVersion(1);
        review.setReviewStatus(ReviewStatus.PENDING.name());
        reviewMapper.insert(review);

        event.setStatus(EventStatus.PENDING_REVIEW.name());
        event.setUpdatedBy(operator.getUserId());
        updateEventWithVersionCheck(event);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.EVENT_SUBMIT_REVIEW,
                OperationAction.TARGET_EVENT, eventId, Map.of("snapshotHash", hash), ip);

        return toResponse(event);
    }

    /** 审核活动：PENDING_REVIEW → APPROVED（通过）/ REJECTED（驳回）。 */
    @Transactional
    public EventResponse review(Long eventId, ReviewRequest req, AuthUser operator, String ip) {
        Event event = requireEvent(eventId);
        if (!EventStatus.PENDING_REVIEW.name().equals(event.getStatus())) {
            throw new BizException(ErrorCode.EVENT_REVIEW_ILLEGAL_STATE);
        }
        EventReview review = requirePendingReview(eventId);
        if (review.getSubmittedBy().equals(operator.getUserId())) {
            throw new BizException(ErrorCode.EVENT_REVIEW_SELF_REVIEW);
        }
        // 冻结规则兜底：审核时重算指纹，防提交后被并发修改
        assertFingerprintMatches(eventId, review.getSnapshotHash());

        review.setReviewerId(operator.getUserId());
        review.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        if (Boolean.TRUE.equals(req.approve())) {
            review.setReviewStatus(ReviewStatus.APPROVED.name());
            review.setReviewComment(trimToNull(req.comment()));
            event.setStatus(EventStatus.APPROVED.name());
        } else {
            if (req.comment() == null || req.comment().isBlank()) {
                throw new BizException(ErrorCode.EVENT_REVIEW_COMMENT_REQUIRED);
            }
            review.setReviewStatus(ReviewStatus.REJECTED.name());
            review.setReviewComment(req.comment().trim());
            event.setStatus(EventStatus.REJECTED.name());
        }
        reviewMapper.updateById(review);
        event.setUpdatedBy(operator.getUserId());
        updateEventWithVersionCheck(event);

        Map<String, Object> auditDetail = new java.util.HashMap<>();
        auditDetail.put("approve", req.approve());
        auditDetail.put("comment", review.getReviewComment());
        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.EVENT_REVIEW,
                OperationAction.TARGET_EVENT, eventId, auditDetail, ip);

        return toResponse(event);
    }

    /** 正式发布：APPROVED → PUBLISHED，重新校验配置完整性且与审核快照一致。 */
    @Transactional
    public EventResponse publish(Long eventId, AuthUser operator, String ip) {
        Event event = requireEvent(eventId);
        if (!EventStatus.APPROVED.name().equals(event.getStatus())) {
            throw new BizException(ErrorCode.EVENT_PUBLISH_ILLEGAL_STATE);
        }
        assertPublishReady(event);
        EventReview review = requireApprovedReview(eventId);
        assertFingerprintMatches(eventId, review.getSnapshotHash());

        event.setStatus(EventStatus.PUBLISHED.name());
        event.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        event.setUpdatedBy(operator.getUserId());
        updateEventWithVersionCheck(event);

        // 发布即初始化库存总账（从审核通过的票档配置生成，见 doc/库存与票档预占模块.md 三）
        initializeInventories(eventId);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.EVENT_PUBLISH,
                OperationAction.TARGET_EVENT, eventId,
                Map.of("snapshotHash", review.getSnapshotHash()), ip);

        return toResponse(event);
    }

    /** 遍历活动的有效场次与票档，逐个初始化库存账本（幂等）。 */
    private void initializeInventories(Long eventId) {
        for (Session session : sessionMapper.findByEventId(eventId)) {
            for (TicketTier tier : tierMapper.findBySessionId(session.getId())) {
                inventoryService.initialize(tier.getId(), tier.getInitialQuantity());
            }
        }
    }

    /* ---------- 校验与内部 ---------- */

    private Event requireEvent(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }

    /** 提交审核完整性：至少一个有效场次，每个场次至少一个票档，每个票档有售票计划。 */
    private void assertSubmitReady(Long eventId) {
        List<Session> sessions = sessionMapper.findByEventId(eventId);
        boolean hasValidSession = false;
        for (Session session : sessions) {
            if (!SessionStatus.SCHEDULED.name().equals(session.getStatus())) {
                continue;
            }
            hasValidSession = true;
            List<TicketTier> tiers = tierMapper.findBySessionId(session.getId());
            if (tiers.isEmpty()) {
                throw new BizException(ErrorCode.EVENT_SUBMIT_NOT_READY);
            }
            for (TicketTier tier : tiers) {
                SalePlan plan = salePlanMapper.findByTierId(tier.getId());
                if (plan == null) {
                    throw new BizException(ErrorCode.EVENT_SUBMIT_NOT_READY);
                }
            }
        }
        if (!hasValidSession) {
            throw new BizException(ErrorCode.EVENT_SUBMIT_NOT_READY);
        }
    }

    /** 发布信息完整性：封面、详情、主办方、观演须知为发布时必填（doc 3.2）。 */
    private void assertPublishReady(Event event) {
        if (isBlank(event.getCoverUrl()) || isBlank(event.getDetailContent())
                || isBlank(event.getOrganizerName()) || isBlank(event.getAttendanceNotice())) {
            throw new BizException(ErrorCode.EVENT_PUBLISH_NOT_READY);
        }
    }

    private EventReview requirePendingReview(Long eventId) {
        EventReview review = reviewMapper.findLatestByEventId(eventId);
        if (review == null || !ReviewStatus.PENDING.name().equals(review.getReviewStatus())) {
            throw new BizException(ErrorCode.EVENT_REVIEW_ILLEGAL_STATE);
        }
        return review;
    }

    private EventReview requireApprovedReview(Long eventId) {
        EventReview review = reviewMapper.findLatestByEventId(eventId);
        if (review == null || !ReviewStatus.APPROVED.name().equals(review.getReviewStatus())) {
            throw new BizException(ErrorCode.EVENT_PUBLISH_ILLEGAL_STATE);
        }
        return review;
    }

    private void assertFingerprintMatches(Long eventId, String snapshotHash) {
        if (!snapshotHash.equals(fingerprint.compute(eventId))) {
            throw new BizException(ErrorCode.EVENT_PUBLISH_FINGERPRINT_MISMATCH);
        }
    }

    private void updateEventWithVersionCheck(Event event) {
        int rows = eventMapper.updateById(event);
        if (rows == 0) {
            throw new BizException(ErrorCode.EVENT_VERSION_CONFLICT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static EventResponse toResponse(Event e) {
        return new EventResponse(
                String.valueOf(e.getId()), String.valueOf(e.getCategoryId()), e.getTitle(), e.getSubtitle(),
                e.getCoverUrl(), e.getDescription(), e.getDetailContent(), e.getOrganizerName(),
                e.getAttendanceNotice(), e.getRefundPolicy(), e.getStatus(), e.getPublishedAt(),
                e.getCancelledAt(), e.getCancelReason(), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
