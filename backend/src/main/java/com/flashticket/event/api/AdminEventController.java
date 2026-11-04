package com.flashticket.event.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.event.dto.EventCancelRequest;
import com.flashticket.event.dto.EventRequest;
import com.flashticket.event.dto.EventResponse;
import com.flashticket.event.dto.ReviewRequest;
import com.flashticket.event.service.EventReviewService;
import com.flashticket.event.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台演出活动管理入口（M05/M06）。
 *
 * <p>活动属演出域，OPERATOR / SUPER_ADMIN 持有 event:event:* 权限点；
 * 取消活动属高风险，仅 SUPER_ADMIN；提交审核由运营发起，审核与发布仅
 * SUPER_ADMIN（见 doc/演出活动与场次.md 九）。
 */
@RestController
@RequestMapping("/api/v1/admin/events")
public class AdminEventController {

    private final EventService eventService;
    private final EventReviewService reviewService;

    public AdminEventController(EventService eventService,
                                EventReviewService reviewService) {
        this.eventService = eventService;
        this.reviewService = reviewService;
    }

    /** 活动分页列表（状态/分类/标题筛选）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('event:event:list')")
    public ApiResponse<PageResult<EventResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(eventService.list(page, pageSize, status, categoryId, keyword));
    }

    /** 活动详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('event:event:list')")
    public ApiResponse<EventResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(eventService.getById(id));
    }

    /** 创建活动草稿。 */
    @PostMapping
    @PreAuthorize("hasAuthority('event:event:create')")
    public ApiResponse<EventResponse> create(@Valid @RequestBody EventRequest request,
                                             HttpServletRequest http) {
        return ApiResponse.ok(eventService.create(
                request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 编辑活动草稿（版本校验）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('event:event:update')")
    public ApiResponse<EventResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody EventRequest request,
                                             HttpServletRequest http) {
        return ApiResponse.ok(eventService.update(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 取消活动。 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('event:event:cancel')")
    public ApiResponse<EventResponse> cancel(@PathVariable Long id,
                                             @Valid @RequestBody EventCancelRequest request,
                                             HttpServletRequest http) {
        return ApiResponse.ok(eventService.cancel(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 提交审核：DRAFT/REJECTED → PENDING_REVIEW。 */
    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAuthority('event:event:submit')")
    public ApiResponse<EventResponse> submitReview(@PathVariable Long id,
                                                   HttpServletRequest http) {
        return ApiResponse.ok(reviewService.submitReview(
                id, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 审核活动：PENDING_REVIEW → APPROVED/REJECTED（仅 SUPER_ADMIN）。 */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('event:event:review')")
    public ApiResponse<EventResponse> review(@PathVariable Long id,
                                             @Valid @RequestBody ReviewRequest request,
                                             HttpServletRequest http) {
        return ApiResponse.ok(reviewService.review(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 正式发布：APPROVED → PUBLISHED（仅 SUPER_ADMIN）。 */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('event:event:publish')")
    public ApiResponse<EventResponse> publish(@PathVariable Long id,
                                              HttpServletRequest http) {
        return ApiResponse.ok(reviewService.publish(
                id, SecurityUtils.currentUser(), clientIp(http)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
