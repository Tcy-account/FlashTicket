package com.flashticket.notification.enums;

/**
 * 通知类型（M14）。
 *
 * <p>对应 ft_notification.type 取值。P0 仅交易类事件接入站内通知，
 * EVENT（演出变更）/MARKETING（营销）留待后续里程碑扩展。
 */
public enum NotificationType {
    TRANSACTION,
    EVENT,
    MARKETING,
    SYSTEM
}
