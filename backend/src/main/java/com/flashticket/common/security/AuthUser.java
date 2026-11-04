package com.flashticket.common.security;

import java.util.List;

/**
 * 已认证主体：由 JWT 解析出的最小身份信息。
 *
 * <p>区别于数据库实体 {@code User}，本类只承载访问令牌中的会话上下文，
 * 供控制器/服务层免查询地获取当前用户 ID 与会话 ID。
 */
public final class AuthUser {

    private final Long userId;
    private final String username;
    private final String sessionId;
    private final List<String> roles;
    private final List<String> permissions;

    public AuthUser(Long userId, String username, String sessionId,
                    List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.roles = roles;
        this.permissions = permissions;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
