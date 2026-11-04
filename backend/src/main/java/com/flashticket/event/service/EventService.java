package com.flashticket.event.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.event.dto.EventCancelRequest;
import com.flashticket.event.dto.EventRequest;
import com.flashticket.event.dto.EventResponse;
import com.flashticket.event.entity.Event;
import com.flashticket.event.entity.EventCategory;
import com.flashticket.event.enums.CategoryStatus;
import com.flashticket.event.enums.EventStatus;
import com.flashticket.event.mapper.EventCategoryMapper;
import com.flashticket.event.mapper.EventMapper;
import com.flashticket.identity.domain.OperationAction;
import com.flashticket.identity.service.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演出活动编排服务（M05）：草稿创建、编辑、列表、详情、取消，及公开已发布查询。
 *
 * <p>规则（见 doc/演出活动与场次.md 三 / doc/全局逻辑ER模型.md 三）：
 * <ul>
 *   <li>M05 只落地 DRAFT 草稿生命周期与 CANCELLED 取消；审核发布留待 M06；</li>
 *   <li>活动状态不等同于售票状态；</li>
 *   <li>version 乐观锁：编辑须回传当前版本，版本不符即拒绝覆盖（防两个运营并发编辑）；</li>
 *   <li>公开查询仅返回 PUBLISHED 活动，草稿不可见。</li>
 * </ul>
 */
@Service
public class EventService {

    private final EventMapper eventMapper;
    private final EventCategoryMapper categoryMapper;
    private final OperationAuditService auditService;

    public EventService(EventMapper eventMapper,
                        EventCategoryMapper categoryMapper,
                        OperationAuditService auditService) {
        this.eventMapper = eventMapper;
        this.categoryMapper = categoryMapper;
        this.auditService = auditService;
    }

    /** 后台活动分页列表（状态/分类/标题筛选）。 */
    public PageResult<EventResponse> list(long page, long pageSize,
                                          String status, Long categoryId, String keyword) {
        Page<Event> p = new Page<>(page, pageSize);
        IPage<Event> result = eventMapper.selectEventPage(p,
                trimToNull(status), categoryId, trimToNull(keyword));
        List<EventResponse> items = result.getRecords().stream()
                .map(EventService::toResponse).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 后台活动详情。 */
    public EventResponse getById(Long id) {
        return toResponse(requireEvent(id));
    }

    /** 创建活动草稿。 */
    @Transactional
    public EventResponse create(EventRequest req, AuthUser operator, String ip) {
        Long categoryId = Long.valueOf(req.categoryId());
        assertCategoryAvailable(categoryId);

        Event event = new Event();
        event.setCategoryId(categoryId);
        event.setTitle(req.title().trim());
        event.setSubtitle(trimToNull(req.subtitle()));
        event.setCoverUrl(trimToNull(req.coverUrl()));
        event.setDescription(trimToNull(req.description()));
        event.setDetailContent(trimToNull(req.detailContent()));
        event.setOrganizerName(trimToNull(req.organizerName()));
        event.setAttendanceNotice(trimToNull(req.attendanceNotice()));
        event.setRefundPolicy(trimToNull(req.refundPolicy()));
        event.setStatus(EventStatus.DRAFT.name());
        event.setCreatedBy(operator.getUserId());
        event.setUpdatedBy(operator.getUserId());
        event.setVersion(0L);
        eventMapper.insert(event);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.EVENT_CREATE,
                OperationAction.TARGET_EVENT, event.getId(),
                Map.of("title", event.getTitle(), "categoryId", event.getCategoryId()), ip);

        return toResponse(event);
    }

    /** 编辑活动（乐观锁版本校验；进入审核/发布流程后配置冻结，见 C05）。 */
    @Transactional
    public EventResponse update(Long id, EventRequest req, AuthUser operator, String ip) {
        Event event = requireEvent(id);
        assertEditable(event);
        assertVersion(event, req.version());
        Long categoryId = Long.valueOf(req.categoryId());
        assertCategoryAvailable(categoryId);

        event.setCategoryId(categoryId);
        event.setTitle(req.title().trim());
        event.setSubtitle(trimToNull(req.subtitle()));
        event.setCoverUrl(trimToNull(req.coverUrl()));
        event.setDescription(trimToNull(req.description()));
        event.setDetailContent(trimToNull(req.detailContent()));
        event.setOrganizerName(trimToNull(req.organizerName()));
        event.setAttendanceNotice(trimToNull(req.attendanceNotice()));
        event.setRefundPolicy(trimToNull(req.refundPolicy()));
        event.setUpdatedBy(operator.getUserId());
        updateWithVersionCheck(event);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.EVENT_UPDATE,
                OperationAction.TARGET_EVENT, id,
                Map.of("title", event.getTitle(), "categoryId", event.getCategoryId()), ip);

        return toResponse(event);
    }

    /** 取消活动（未发布的活动可取消；已发布活动取消涉及订单退款，留待交易里程碑）。 */
    @Transactional
    public EventResponse cancel(Long id, EventCancelRequest req, AuthUser operator, String ip) {
        Event event = requireEvent(id);
        String status = event.getStatus();
        if (EventStatus.PUBLISHED.name().equals(status)
                || EventStatus.CANCELLED.name().equals(status)
                || EventStatus.ARCHIVED.name().equals(status)) {
            throw new BizException(ErrorCode.EVENT_NOT_CANCELLABLE);
        }

        event.setStatus(EventStatus.CANCELLED.name());
        event.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));
        event.setCancelReason(req.reason().trim());
        event.setUpdatedBy(operator.getUserId());
        updateWithVersionCheck(event);

        auditService.record(operator.getUserId(), operator.getUsername(), OperationAction.EVENT_CANCEL,
                OperationAction.TARGET_EVENT, id, Map.of("reason", event.getCancelReason()), ip);

        return toResponse(event);
    }

    /** 公开活动分页（仅 PUBLISHED）。 */
    public PageResult<EventResponse> listPublished(long page, long pageSize,
                                                   Long categoryId, String keyword) {
        Page<Event> p = new Page<>(page, pageSize);
        IPage<Event> result = eventMapper.selectPublishedPage(p, categoryId, trimToNull(keyword));
        List<EventResponse> items = result.getRecords().stream()
                .map(EventService::toResponse).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 公开活动详情（仅 PUBLISHED，草稿/取消不可见）。 */
    public EventResponse getPublished(Long id) {
        Event event = requireEvent(id);
        if (!EventStatus.PUBLISHED.name().equals(event.getStatus())) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        return toResponse(event);
    }

    /* ---------- 内部 ---------- */

    private Event requireEvent(Long id) {
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }

    private void assertEditable(Event event) {
        String status = event.getStatus();
        if (!EventStatus.DRAFT.name().equals(status) && !EventStatus.REJECTED.name().equals(status)) {
            throw new BizException(ErrorCode.EVENT_CONFIG_FROZEN);
        }
    }

    /** 乐观锁：编辑须回传当前版本，版本不符拒绝覆盖。 */
    private void assertVersion(Event event, Long requestVersion) {
        if (requestVersion == null || !Objects.equals(event.getVersion(), requestVersion)) {
            throw new BizException(ErrorCode.EVENT_VERSION_CONFLICT);
        }
    }

    /** updateById 并校验受影响行数（@Version 乐观锁兜底，防止并发覆盖）。 */
    private void updateWithVersionCheck(Event event) {
        int rows = eventMapper.updateById(event);
        if (rows == 0) {
            throw new BizException(ErrorCode.EVENT_VERSION_CONFLICT);
        }
    }

    private void assertCategoryAvailable(Long categoryId) {
        EventCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(ErrorCode.EVENT_CATEGORY_NOT_FOUND);
        }
        if (!CategoryStatus.ENABLED.name().equals(category.getStatus())) {
            throw new BizException(ErrorCode.EVENT_CATEGORY_DISABLED);
        }
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
