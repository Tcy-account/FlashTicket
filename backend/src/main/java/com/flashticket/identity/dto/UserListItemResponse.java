package com.flashticket.identity.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户列表项（雪花 ID 以字符串传输）。
 */
public record UserListItemResponse(
        String id,
        String username,
        String email,
        String nickname,
        String status,
        List<String> roles,
        LocalDateTime createdAt) {
}
