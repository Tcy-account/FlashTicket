package com.flashticket.identity.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.security.AuthUser;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.identity.dto.AdminResetPasswordRequest;
import com.flashticket.identity.dto.AssignRolesRequest;
import com.flashticket.identity.dto.CreateStaffRequest;
import com.flashticket.identity.dto.UpdateStaffRequest;
import com.flashticket.identity.dto.UpdateUserStatusRequest;
import com.flashticket.identity.dto.UserListItemResponse;
import com.flashticket.identity.service.AdminUserService;
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
 * 后台账号管理入口（M03）：用户列表、员工 CRUD、启用禁用、角色分配、强制下线、重置密码。
 *
 * <p>所有接口均方法级授权到权限点（admin:user:*），普通用户无权限点 → 403。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminUsersController {

    private final AdminUserService adminUserService;

    public AdminUsersController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /** 用户列表（分页 + 筛选）。 */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('admin:user:list')")
    public ApiResponse<PageResult<UserListItemResponse>> listUsers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        return ApiResponse.ok(adminUserService.listUsers(page, pageSize, username, status, role));
    }

    /** 创建员工。 */
    @PostMapping("/staff")
    @PreAuthorize("hasAuthority('admin:user:create')")
    public ApiResponse<UserListItemResponse> createStaff(@Valid @RequestBody CreateStaffRequest request,
                                                         HttpServletRequest http) {
        AuthUser operator = SecurityUtils.currentUser();
        return ApiResponse.ok(adminUserService.createStaff(request, operator, clientIp(http)));
    }

    /** 修改员工（昵称/邮箱）。 */
    @PutMapping("/staff/{id}")
    @PreAuthorize("hasAuthority('admin:user:update')")
    public ApiResponse<UserListItemResponse> updateStaff(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateStaffRequest request,
                                                         HttpServletRequest http) {
        AuthUser operator = SecurityUtils.currentUser();
        return ApiResponse.ok(adminUserService.updateStaff(id, request, operator, clientIp(http)));
    }

    /** 启用/禁用账号。 */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('admin:user:status')")
    public ApiResponse<Void> changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateUserStatusRequest request,
                                          HttpServletRequest http) {
        adminUserService.changeStatus(id, request, SecurityUtils.currentUser(), clientIp(http));
        return ApiResponse.ok();
    }

    /** 分配角色（整体替换）。 */
    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('admin:user:roles')")
    public ApiResponse<Void> assignRoles(@PathVariable Long id,
                                         @Valid @RequestBody AssignRolesRequest request,
                                         HttpServletRequest http) {
        adminUserService.assignRoles(id, request, SecurityUtils.currentUser(), clientIp(http));
        return ApiResponse.ok();
    }

    /** 强制下线：撤销指定用户全部会话。 */
    @PostMapping("/users/{id}/revoke-sessions")
    @PreAuthorize("hasAuthority('admin:user:revoke')")
    public ApiResponse<Void> revokeSessions(@PathVariable Long id, HttpServletRequest http) {
        adminUserService.revokeSessions(id, SecurityUtils.currentUser(), clientIp(http));
        return ApiResponse.ok();
    }

    /** 管理员重置员工密码（不返回明文）。 */
    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('admin:user:reset-pwd')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody AdminResetPasswordRequest request,
                                           HttpServletRequest http) {
        adminUserService.resetPassword(id, request, SecurityUtils.currentUser(), clientIp(http));
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
