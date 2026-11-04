package com.flashticket.identity.dto;

import java.util.List;

/**
 * 配置角色权限点请求（permissionId 列表，整体替换）。
 */
public record GrantRolePermissionsRequest(List<Long> permissionIds) {
}
