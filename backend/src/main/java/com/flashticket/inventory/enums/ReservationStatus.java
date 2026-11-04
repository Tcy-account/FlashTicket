package com.flashticket.inventory.enums;

/**
 * 库存预占状态机（表 ft_reservation.status），见 doc/库存与票档预占模块.md 四。
 *
 * <p>主要成功路径：RESERVED → CONFIRMED（支付成功转已售）或 RELEASED（取消/超时释放）。
 * 重复确认、重复释放均幂等返回，不允许 CONFIRMED ↔ RELEASED 互转。
 */
public enum ReservationStatus {
    /** 已由 MySQL 确认预占，等待支付结果。 */
    RESERVED,
    /** 支付成功，转已售。 */
    CONFIRMED,
    /** 取消、超时或失败释放。 */
    RELEASED
}
