package com.flashticket.identity.dto;

import java.util.List;

/**
 * 角色视图（雪花 ID 以字符串传输）。
 */
public record RoleResponse(String id, String code, String name, String status) {
}
