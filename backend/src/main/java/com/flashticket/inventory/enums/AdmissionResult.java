package com.flashticket.inventory.enums;

/**
 * Redis 高速库存准入决策（M15-C）。
 *
 * <p>ADMIT=预减成功可受理；SOLD_OUT=Redis 快照判定库存不足，入口快速拒绝；
 * FALLBACK=快照不存在/Redis 不可用，回退 MySQL 权威账本直接受理。
 */
public enum AdmissionResult {
    ADMIT,
    SOLD_OUT,
    FALLBACK
}
