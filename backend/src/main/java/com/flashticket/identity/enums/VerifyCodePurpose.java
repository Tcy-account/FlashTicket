package com.flashticket.identity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 邮箱验证码用途（互相隔离，见 doc/用户与账号模块设计.md 3.1）。
 *
 * <p>不同用途的验证码使用不同 Redis key 前缀，注册验证码不能用于找回密码等场景。
 */
public enum VerifyCodePurpose {
    /** 注册。 */
    REGISTER,
    /** 找回密码。 */
    RESET_PASSWORD,
    /** 换绑邮箱。 */
    CHANGE_EMAIL;

    /** 反序列化时忽略大小写与首尾空白。 */
    @JsonCreator
    public static VerifyCodePurpose from(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
