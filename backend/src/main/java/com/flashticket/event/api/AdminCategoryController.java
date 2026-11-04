package com.flashticket.event.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.event.dto.CategoryRequest;
import com.flashticket.event.dto.CategoryResponse;
import com.flashticket.event.dto.CategoryStatusRequest;
import com.flashticket.event.service.EventCategoryService;
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
 * 后台演出分类管理入口（M04）。
 *
 * <p>分类管理属演出域，OPERATOR / SUPER_ADMIN 持有 event:category:* 权限点。
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final EventCategoryService categoryService;

    public AdminCategoryController(EventCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 分类列表（含停用）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('event:category:list')")
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(categoryService.listAll());
    }

    /** 创建分类。 */
    @PostMapping
    @PreAuthorize("hasAuthority('event:category:create')")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request,
                                                HttpServletRequest http) {
        return ApiResponse.ok(categoryService.create(
                request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 编辑分类。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('event:category:update')")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CategoryRequest request,
                                                HttpServletRequest http) {
        return ApiResponse.ok(categoryService.update(
                id, request, SecurityUtils.currentUser(), clientIp(http)));
    }

    /** 启用/停用分类。 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('event:category:toggle')")
    public ApiResponse<Void> toggle(@PathVariable Long id,
                                    @Valid @RequestBody CategoryStatusRequest request,
                                    HttpServletRequest http) {
        categoryService.toggle(id, request.status(), SecurityUtils.currentUser(), clientIp(http));
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
