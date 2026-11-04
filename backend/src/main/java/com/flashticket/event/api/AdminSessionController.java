package com.flashticket.event.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.event.dto.SessionCancelRequest;
import com.flashticket.event.dto.SessionPostponeRequest;
import com.flashticket.event.dto.SessionRequest;
import com.flashticket.event.dto.SessionResponse;
import com.flashticket.event.service.EventSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台演出场次管理入口（M05）。
 *
 * <p>场次属演出域，OPERATOR / SUPER_ADMIN 持有 event:session:* 权限点；
 * 取消场次属高风险，仅 SUPER_ADMIN（见 doc/演出活动与场次.md 九）。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminSessionController {

    private final EventSessionService sessionService;

    public AdminSessionController(EventSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** 某活动的场次列表。 */
    @GetMapping("/events/{eventId}/sessions")
    @PreAuthorize("hasAuthority('event:session:list')")
    public ApiResponse<List<SessionResponse>> list(@PathVariable Long eventId) {
        return ApiResponse.ok(sessionService.listByEvent(eventId));
    }

    /** 创建场次（含场馆时间冲突校验）。 */
    @PostMapping("/events/{eventId}/sessions")
    @PreAuthorize("hasAuthority('event:session:create')")
    public ApiResponse<SessionResponse> create(@PathVariable Long eventId,
                                               @Valid @RequestBody SessionRequest request,
                                               HttpServletRequest http) {
        return ApiResponse.ok(sessionService.create(
                eventId, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 编辑场次（版本校验 + 冲突校验）。 */
    @PutMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('event:session:update')")
    public ApiResponse<SessionResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody SessionRequest request,
                                               HttpServletRequest http) {
        return ApiResponse.ok(sessionService.update(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 延期场次。 */
    @PostMapping("/sessions/{id}/postpone")
    @PreAuthorize("hasAuthority('event:session:postpone')")
    public ApiResponse<SessionResponse> postpone(@PathVariable Long id,
                                                 @Valid @RequestBody SessionPostponeRequest request,
                                                 HttpServletRequest http) {
        return ApiResponse.ok(sessionService.postpone(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 取消场次。 */
    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize("hasAuthority('event:session:cancel')")
    public ApiResponse<SessionResponse> cancel(@PathVariable Long id,
                                               @Valid @RequestBody SessionCancelRequest request,
                                               HttpServletRequest http) {
        return ApiResponse.ok(sessionService.cancel(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
