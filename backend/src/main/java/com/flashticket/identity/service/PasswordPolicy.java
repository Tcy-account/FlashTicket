package com.flashticket.identity.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;

import java.util.Locale;
import java.util.Set;

/**
 * 密码强度策略（注册/改密/重置共用）。
 *
 * <p>实现「拒绝常见或已泄露密码」（doc/用户与账号模块设计.md 3.1 / 6.1）：
 * 仅拦截明显常见的弱密码，不强制包含所有字符类型，兼容密码管理器生成的复杂密码。
 */
public final class PasswordPolicy {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "123456789012", "1234567890123", "12345678901234", "123456789012345",
            "password1234", "password12345", "password123456",
            "qwertyuiopas", "qwertyuiopasdf",
            "aaaaaaaaaaaa", "bbbbbbbbbbbb",
            "111111111111", "000000000000",
            "iloveyou1234", "letmein12345", "admin1234567");

    private PasswordPolicy() {
    }

    /** 校验密码强度，过弱则抛 PARAM_ERROR。 */
    public static void assertStrong(String password) {
        if (password != null && COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密码过于常见，请使用更安全的密码");
        }
    }
}
