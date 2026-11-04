package com.flashticket.identity.enums;

/**
 * 账号主状态（表 ft_user.status）。
 *
 * <p>与登录失败触发的临时锁定（LOCKED）严格区分：临时锁定属登录保护状态，
 * 不落入账号主状态枚举（见 doc/用户与账号模块设计.md 7.1）。
 */
public enum UserStatus {
    /** 正常，允许登录。 */
    ACTIVE,
    /** 管理员停用，禁止登录。 */
    DISABLED,
    /** 注销处理中，禁止登录。 */
    DELETION_PENDING,
    /** 已完成删除或不可逆匿名化，禁止登录。 */
    DELETED
}
