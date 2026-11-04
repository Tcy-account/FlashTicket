package com.flashticket.event.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.event.dto.VenueRequest;
import com.flashticket.event.dto.VenueResponse;
import com.flashticket.event.dto.VenueStatusRequest;
import com.flashticket.event.service.VenueService;
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
 * 后台演出场馆管理入口（M04）。
 *
 * <p>场馆管理属演出域，OPERATOR / SUPER_ADMIN 持有 event:venue:* 权限点。
 */
@RestController
@RequestMapping("/api/v1/admin/venues")
public class AdminVenueController {

    private final VenueService venueService;

    public AdminVenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    /** 场馆分页列表（城市/状态/名称筛选）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('event:venue:list')")
    public ApiResponse<PageResult<VenueResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name) {
        return ApiResponse.ok(venueService.list(page, pageSize, city, status, name));
    }

    /** 创建场馆。 */
    @PostMapping
    @PreAuthorize("hasAuthority('event:venue:create')")
    public ApiResponse<VenueResponse> create(@Valid @RequestBody VenueRequest request,
                                             HttpServletRequest http) {
        return ApiResponse.ok(venueService.create(
                request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 编辑场馆。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('event:venue:update')")
    public ApiResponse<VenueResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody VenueRequest request,
                                             HttpServletRequest http) {
        return ApiResponse.ok(venueService.update(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 启用/停用场馆。 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('event:venue:toggle')")
    public ApiResponse<Void> toggle(@PathVariable Long id,
                                    @Valid @RequestBody VenueStatusRequest request,
                                    HttpServletRequest http) {
        venueService.toggle(id, request.status(), SecurityUtils.currentUser(), clientIp(http));
        return ApiResponse.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
