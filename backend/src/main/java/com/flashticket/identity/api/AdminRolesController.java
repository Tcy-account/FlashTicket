package com.flashticket.identity.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.common.dto.PageResult;
import com.flashticket.identity.dto.AuditLogResponse;
import com.flashticket.identity.dto.GrantRolePermissionsRequest;
import com.flashticket.identity.dto.PermissionResponse;
import com.flashticket.identity.dto.RoleResponse;
import com.flashticket.identity.service.RolePermissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与权限管理入口（M03）：角色列表、权限点列表、角色权限配置、审计日志。
 *
 * <p>「配置角色权限」为全局授权，仅 SUPER_ADMIN 拥有 admin:role:grant 权限点；
 * ADMIN 只能查看角色/权限/审计。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminRolesController {

    private final RolePermissionService rolePermissionService;

    public AdminRolesController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /** 角色列表。 */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('admin:role:list')")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.ok(rolePermissionService.listRoles());
    }

    /** 权限点列表。 */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('admin:permission:list')")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.ok(rolePermissionService.listPermissions());
    }

    /** 某角色已绑定的权限点。 */
    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('admin:role:list')")
    public ApiResponse<List<PermissionResponse>> getRolePermissions(@PathVariable Long id) {
        return ApiResponse.ok(rolePermissionService.getRolePermissions(id));
    }

    /** 配置角色权限点（仅 SUPER_ADMIN）。 */
    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('admin:role:grant')")
    public ApiResponse<Void> grantRolePermissions(@PathVariable Long id,
                                                  @RequestBody GrantRolePermissionsRequest request,
                                                  HttpServletRequest http) {
        rolePermissionService.grantRolePermissions(id, request.permissionIds(),
                SecurityUtils.currentUser(), clientIp(http));
        return ApiResponse.ok();
    }

    /** 审计日志分页查询。 */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('admin:audit:list')")
    public ApiResponse<PageResult<AuditLogResponse>> listAuditLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(rolePermissionService.listAuditLogs(page, pageSize));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
