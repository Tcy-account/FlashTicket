package com.flashticket.identity.domain;

/**
 * 领域服务创建注册用户的产出（内部对象，非对外 DTO）。
 */
public record RegisteredUser(
        Long userId,
        String username,
        String email,
        String nickname) {
}
