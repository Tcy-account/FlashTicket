package com.flashticket.identity.dto;

import java.util.List;

/**
 * 用户资料摘要（雪花 ID 以字符串传输）。
 */
public record UserProfileResponse(
        String id,
        String username,
        String nickname,
        String email,
        String avatarUrl,
        List<String> roles,
        List<String> permissions) {
}
