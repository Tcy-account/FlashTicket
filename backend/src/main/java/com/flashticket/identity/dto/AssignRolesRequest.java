package com.flashticket.identity.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 分配角色请求（整体替换用户角色，roleId 列表）。
 */
public record AssignRolesRequest(
        @NotEmpty(message = "至少分配一个角色")
        List<Long> roleIds) {
}
