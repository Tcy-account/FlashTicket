package com.flashticket.identity.domain;

import java.util.Map;
import java.util.Set;

/**
 * 角色码常量与角色层级（RBAC 授权边界依据）。
 *
 * <p>五种正式角色见 doc/用户与账号模块设计.md 八：
 * USER / OPERATOR / FINANCE / ADMIN / SUPER_ADMIN。
 *
 * <p>后台角色集合用于判定后台登录资格；rank 用于越权校验——
 * 操作者只能授予不高于自身最高层级的目标角色，且只有 SUPER_ADMIN
 * 能授予 SUPER_ADMIN。
 */
public final class RoleCode {

    public static final String USER = "USER";
    public static final String OPERATOR = "OPERATOR";
    public static final String FINANCE = "FINANCE";
    public static final String ADMIN = "ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** 具备后台登录权的角色（普通 USER 不属于后台角色）。 */
    public static final Set<String> BACKEND_ROLES =
            Set.of(OPERATOR, FINANCE, ADMIN, SUPER_ADMIN);

    /** 角色层级：数字越大权限越高（仅用于 RBAC 越权校验，不代表功能全集）。 */
    private static final Map<String, Integer> RANK = Map.of(
            USER, 0,
            OPERATOR, 1,
            FINANCE, 1,
            ADMIN, 2,
            SUPER_ADMIN, 3);

    private RoleCode() {
    }

    /** 判断是否为后台角色。 */
    public static boolean isBackendRole(String code) {
        return code != null && BACKEND_ROLES.contains(code);
    }

    /** 角色层级，未知角色返回 0。 */
    public static int rank(String code) {
        return RANK.getOrDefault(code, 0);
    }
}
