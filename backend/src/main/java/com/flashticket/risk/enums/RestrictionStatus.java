package com.flashticket.risk.enums;

/**
 * 风控限制状态机（M15-B）：ACTIVE 生效 → EXPIRED 到期自动解除 / REVOKED 管理员提前解除。
 */
public enum RestrictionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
