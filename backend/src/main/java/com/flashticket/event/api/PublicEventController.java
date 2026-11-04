package com.flashticket.event.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.event.dto.CategoryResponse;
import com.flashticket.event.dto.EventResponse;
import com.flashticket.event.dto.SessionResponse;
import com.flashticket.event.dto.TicketTierResponse;
import com.flashticket.event.dto.TierAvailabilityResponse;
import com.flashticket.event.dto.VenueResponse;
import com.flashticket.event.service.EventCategoryService;
import com.flashticket.event.service.EventService;
import com.flashticket.event.service.EventSessionService;
import com.flashticket.event.service.TicketTierService;
import com.flashticket.event.service.VenueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 演出域公开查询入口（M04/M05/M06）：游客可读取启用的分类、场馆，
 * 以及已发布的活动、正常场次与票档可售状态。
 *
 * <p>对应 doc/演出活动与场次.md 十二：GET /api/v1/categories、/venues、/events、
 * /events/{id}、/events/{id}/sessions、/sessions/{id}/tiers、/tiers/{id}/availability。
 */
@RestController
@RequestMapping("/api/v1")
public class PublicEventController {

    private final EventCategoryService categoryService;
    private final VenueService venueService;
    private final EventService eventService;
    private final EventSessionService sessionService;
    private final TicketTierService tierService;

    public PublicEventController(EventCategoryService categoryService,
                                 VenueService venueService,
                                 EventService eventService,
                                 EventSessionService sessionService,
                                 TicketTierService tierService) {
        this.categoryService = categoryService;
        this.venueService = venueService;
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.tierService = tierService;
    }

    /** 启用分类列表（供用户端分类筛选）。 */
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.ok(categoryService.listEnabled());
    }

    /** 启用场馆列表（可选城市筛选）。 */
    @GetMapping("/venues")
    public ApiResponse<PageResult<VenueResponse>> listVenues(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String city) {
        return ApiResponse.ok(venueService.listEnabled(page, pageSize, city));
    }

    /** 已发布活动列表（分类/标题筛选，草稿不可见）。 */
    @GetMapping("/events")
    public ApiResponse<PageResult<EventResponse>> listEvents(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(eventService.listPublished(page, pageSize, categoryId, keyword));
    }

    /** 已发布活动详情。 */
    @GetMapping("/events/{id}")
    public ApiResponse<EventResponse> getEvent(@PathVariable Long id) {
        return ApiResponse.ok(eventService.getPublished(id));
    }

    /** 某活动的正常场次列表（仅 SCHEDULED）。 */
    @GetMapping("/events/{id}/sessions")
    public ApiResponse<List<SessionResponse>> listSessions(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.listPublishedByEvent(id));
    }

    /** 某场次的票档列表（仅活动已发布且场次正常）。 */
    @GetMapping("/sessions/{id}/tiers")
    public ApiResponse<List<TicketTierResponse>> listTiers(@PathVariable Long id) {
        return ApiResponse.ok(tierService.listPublishedBySession(id));
    }

    /** 票档可售状态（三层销售状态与资格结论）。 */
    @GetMapping("/tiers/{id}/availability")
    public ApiResponse<TierAvailabilityResponse> availability(@PathVariable Long id) {
        return ApiResponse.ok(tierService.getAvailability(id));
    }
}
