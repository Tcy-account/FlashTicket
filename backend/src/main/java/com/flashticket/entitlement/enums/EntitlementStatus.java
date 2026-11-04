package com.flashticket.entitlement.enums;

/**
 * 票务权益状态（表 ft_ticket_entitlement.status）。
 *
 * <p>VALID 有效可用；USED 已核销使用；REVOKED 已撤销（退款，M11 落地）。
 */
public enum EntitlementStatus {
    /** 有效，可正常入场使用。 */
    VALID,
    /** 已核销使用。 */
    USED,
    /** 已撤销（退款等原因，不可继续使用）。 */
    REVOKED
}
