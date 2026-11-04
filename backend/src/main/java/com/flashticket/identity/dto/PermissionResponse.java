package com.flashticket.identity.dto;

/**
 * 权限点视图（雪花 ID 以字符串传输）。
 */
public record PermissionResponse(String id, String code, String name, String type) {
}
