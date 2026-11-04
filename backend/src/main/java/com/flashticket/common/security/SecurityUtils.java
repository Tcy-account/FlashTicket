package com.flashticket.common.security;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具：从 SecurityContext 读取 JWT 认证主体。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 返回当前登录用户，未认证抛 401。 */
    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    /** 返回当前登录用户 ID，未认证抛 401。 */
    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    /** 返回当前登录会话 ID，未认证抛 401。 */
    public static String currentSessionId() {
        return currentUser().getSessionId();
    }
}
