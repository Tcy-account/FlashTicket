package com.flashticket.event.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.SessionCancelRequest;
import com.flashticket.event.dto.SessionPostponeRequest;
import com.flashticket.event.dto.SessionRequest;
import com.flashticket.event.dto.SessionResponse;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.Session;
import com.flashticket.event.entity.Venue;
import com.flashticket.event.enums.EventStatus;
import com.flashticket.event.enums.SessionStatus;
import com.flashticket.event.enums.VenueStatus;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.event.mapper.SessionMapper;
import com.flashticket.event.mapper.VenueMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演出场次编排服务（M05）：创建、编辑、延期、取消、列表，及场馆时间冲突校验。
 *
 * <p>并发关键（见 doc/演出活动与场次.md 5.3 / doc/全局逻辑ER模型.md 五）：
 * 场馆排期冲突校验采用「本地事务 + SELECT ... FOR UPDATE 锁场馆行」串行化，
 * 而非先查后插，避免两个并发创建各自看到「无冲突」却合起来冲突。
 * 更换场馆时按场馆 ID 固定顺序加锁，降低死锁风险。
 */
@Service
public class EventSessionService {

    private final SessionMapper sessionMapper;
    private final EventMapper eventMapper;
    private final VenueMapper venueMapper;
    private final OperationAuditService auditService;

    /** 清场缓冲分钟数（两场次间最短间隔，默认 30）。 */
    @Value("${flashticket.event.session.clear-buffer-minutes:30}")
    private int clearBufferMinutes;

    public EventSessionService(SessionMapper sessionMapper,
                               EventMapper eventMapper,
                               VenueMapper venueMapper,
                               OperationAuditService auditService) {
        this.sessionMapper = sessionMapper;
        this.eventMapper = eventMapper;
        this.venueMapper = venueMapper;
        this.auditService = auditService;
    }

    /** 某活动的全部场次（后台，按开始时间升序）。 */
    public List<SessionResponse> listByEvent(Long eventId) {
        requireEventExists(eventId);
        return sessionMapper.findByEventId(eventId).stream()
                .map(EventSessionService::toResponse).toList();
    }

    /** 创建场次。 */
    @Transactional
    public SessionResponse create(Long eventId, SessionRequest req, AuthUser operator, String ip) {
        requireEventEditable(eventId);
        Long venueId = Long.valueOf(req.venueId());
        Venue venue = requireVenue(venueId);
        validateTime(req.startAt(), req.endAt(), req.entryStartAt());
        assertCapacity(req.saleCapacity(), venue);

        // 场馆排期串行化：锁场馆行后做范围冲突校验
        sessionMapper.lockVenue(venue.getId());
        assertNoTimeConflict(venue.getId(), null, req.startAt(), req.endAt());

        Session session = new Session();
        session.setEventId(eventId);
        session.setVenueId(venueId);
        session.setName(req.name().trim());
        session.setStartAt(req.startAt());
        session.setEndAt(req.endAt());
        session.setEntryStartAt(req.entryStartAt());
        session.setSaleCapacity(req.saleCapacity());
        session.setStatus(SessionStatus.SCHEDULED.name());
        session.setVersion(0L);
        sessionMapper.insert(session);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SESSION_CREATE,
                OperationAction.TARGET_SESSION, session.getId(),
                Map.of("eventId", eventId, "venueId", venue.getId(),
                        "startAt", session.getStartAt(), "endAt", session.getEndAt()), ip);

        return toResponse(session);
    }

    /** 编辑场次（仅 SCHEDULED 可编辑，乐观锁版本校验）。 */
    @Transactional
    public SessionResponse update(Long sessionId, SessionRequest req, AuthUser operator, String ip) {
        Session session = requireSession(sessionId);
        assertStatus(session, SessionStatus.SCHEDULED);
        requireEventEditable(session.getEventId());
        assertVersion(session, req.version());

        Venue newVenue = requireVenue(Long.valueOf(req.venueId()));
        validateTime(req.startAt(), req.endAt(), req.entryStartAt());
        assertCapacity(req.saleCapacity(), newVenue);

        // 涉及场馆按 ID 升序加锁，再做冲突校验（旧场馆释放、新场馆占用）
        lockVenuesInOrder(session.getVenueId(), newVenue.getId());
        boolean venueChanged = !Objects.equals(session.getVenueId(), newVenue.getId());
        Long conflictCheckVenue = newVenue.getId();
        Long conflictExcludeId = venueChanged ? null : session.getId();
        assertNoTimeConflict(conflictCheckVenue, conflictExcludeId, req.startAt(), req.endAt());

        session.setVenueId(newVenue.getId());
        session.setName(req.name().trim());
        session.setStartAt(req.startAt());
        session.setEndAt(req.endAt());
        session.setEntryStartAt(req.entryStartAt());
        session.setSaleCapacity(req.saleCapacity());
        updateWithVersionCheck(session);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SESSION_UPDATE,
                OperationAction.TARGET_SESSION, sessionId,
                Map.of("venueId", session.getVenueId(),
                        "startAt", session.getStartAt(), "endAt", session.getEndAt()), ip);

        return toResponse(session);
    }

    /** 延期场次：SCHEDULED → POSTPONED（原因必填）。 */
    @Transactional
    public SessionResponse postpone(Long sessionId, SessionPostponeRequest req,
                                    AuthUser operator, String ip) {
        Session session = requireSession(sessionId);
        assertStatus(session, SessionStatus.SCHEDULED);

        session.setStatus(SessionStatus.POSTPONED.name());
        session.setPostponedAt(LocalDateTime.now(ZoneOffset.UTC));
        session.setChangeReason(req.reason().trim());
        updateWithVersionCheck(session);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SESSION_POSTPONE,
                OperationAction.TARGET_SESSION, sessionId, Map.of("reason", session.getChangeReason()), ip);

        return toResponse(session);
    }

    /** 取消场次：SCHEDULED / POSTPONED → CANCELLED（原因必填）。 */
    @Transactional
    public SessionResponse cancel(Long sessionId, SessionCancelRequest req,
                                  AuthUser operator, String ip) {
        Session session = requireSession(sessionId);
        if (session.getStatus().equals(SessionStatus.CANCELLED.name())) {
            throw new BizException(ErrorCode.EVENT_SESSION_ILLEGAL_TRANSITION);
        }
        if (session.getStatus().equals(SessionStatus.COMPLETED.name())) {
            throw new BizException(ErrorCode.EVENT_SESSION_ILLEGAL_TRANSITION);
        }

        session.setStatus(SessionStatus.CANCELLED.name());
        session.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));
        session.setChangeReason(req.reason().trim());
        updateWithVersionCheck(session);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.SESSION_CANCEL,
                OperationAction.TARGET_SESSION, sessionId, Map.of("reason", session.getChangeReason()), ip);

        return toResponse(session);
    }

    /** 公开：某活动的正常场次（仅 SCHEDULED），未发布活动不公开场次。 */
    public List<SessionResponse> listPublishedByEvent(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null || !EventStatus.PUBLISHED.name().equals(event.getStatus())) {
            return List.of();
        }
        return sessionMapper.findByEventId(eventId).stream()
                .filter(s -> SessionStatus.SCHEDULED.name().equals(s.getStatus()))
                .map(EventSessionService::toResponse).toList();
    }

    /* ---------- 校验与内部 ---------- */

    private Session requireSession(Long id) {
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BizException(ErrorCode.EVENT_SESSION_NOT_FOUND);
        }
        return session;
    }

    private void requireEventExists(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
    }

    /** 活动配置冻结：仅草稿/审核驳回状态可新增或编辑场次（见跨域审查 C05）。 */
    private void requireEventEditable(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        String status = event.getStatus();
        if (!EventStatus.DRAFT.name().equals(status) && !EventStatus.REJECTED.name().equals(status)) {
            throw new BizException(ErrorCode.EVENT_CONFIG_FROZEN);
        }
    }

    private Venue requireVenue(Long venueId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new BizException(ErrorCode.EVENT_VENUE_NOT_FOUND);
        }
        if (!VenueStatus.ENABLED.name().equals(venue.getStatus())) {
            throw new BizException(ErrorCode.EVENT_SESSION_ILLEGAL_TRANSITION);
        }
        return venue;
    }

    private void validateTime(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime entryStartAt) {
        if (!endAt.isAfter(startAt)) {
            throw new BizException(ErrorCode.EVENT_SESSION_INVALID_TIME);
        }
        if (entryStartAt != null && !entryStartAt.isBefore(startAt)) {
            throw new BizException(ErrorCode.EVENT_SESSION_ENTRY_INVALID);
        }
    }

    private void assertCapacity(Integer saleCapacity, Venue venue) {
        if (saleCapacity > venue.getMaxCapacity()) {
            throw new BizException(ErrorCode.EVENT_SESSION_CAPACITY_EXCEED);
        }
    }

    /** 场馆排期串行化 + 时间冲突校验（须在事务内、持有场馆锁后调用）。 */
    private void assertNoTimeConflict(Long venueId, Long excludeSessionId,
                                      LocalDateTime startAt, LocalDateTime endAt) {
        long conflicts = sessionMapper.countTimeConflict(
                venueId, excludeSessionId, startAt, endAt, clearBufferMinutes);
        if (conflicts > 0) {
            throw new BizException(ErrorCode.EVENT_SESSION_TIME_CONFLICT);
        }
    }

    /** 按场馆 ID 升序加排他锁，规避多场馆操作的死锁。 */
    private void lockVenuesInOrder(Long venueA, Long venueB) {
        if (venueA < venueB) {
            sessionMapper.lockVenue(venueA);
            sessionMapper.lockVenue(venueB);
        } else if (venueA > venueB) {
            sessionMapper.lockVenue(venueB);
            sessionMapper.lockVenue(venueA);
        } else {
            sessionMapper.lockVenue(venueA);
        }
    }

    private void assertStatus(Session session, SessionStatus expected) {
        if (!expected.name().equals(session.getStatus())) {
            throw new BizException(ErrorCode.EVENT_SESSION_ILLEGAL_TRANSITION);
        }
    }

    /** 乐观锁：编辑须回传当前版本。 */
    private void assertVersion(Session session, Long requestVersion) {
        if (requestVersion == null || !Objects.equals(session.getVersion(), requestVersion)) {
            throw new BizException(ErrorCode.EVENT_SESSION_VERSION_CONFLICT);
        }
    }

    private void updateWithVersionCheck(Session session) {
        int rows = sessionMapper.updateById(session);
        if (rows == 0) {
            throw new BizException(ErrorCode.EVENT_SESSION_VERSION_CONFLICT);
        }
    }

    private static SessionResponse toResponse(Session s) {
        return new SessionResponse(
                String.valueOf(s.getId()), String.valueOf(s.getEventId()), String.valueOf(s.getVenueId()),
                s.getName(),
                s.getStartAt(), s.getEndAt(), s.getEntryStartAt(), s.getSaleCapacity(),
                s.getStatus(), s.getPostponedAt(), s.getCancelledAt(), s.getChangeReason(),
                s.getVersion(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
