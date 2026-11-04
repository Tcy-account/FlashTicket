package com.flashticket.identity.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 注册成功响应。
 */
public class RegisterResponse {

    /** 雪花 ID，前端以字符串传输（契约 C09）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String username;

    private String email;

    private String nickname;

    public RegisterResponse() {
    }

    public RegisterResponse(Long userId, String username, String email, String nickname) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
