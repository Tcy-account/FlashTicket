package com.flashticket.inventory.enums;

/**
 * 库存流水操作类型（表 ft_inventory_ledger.operation）。
 *
 * <p>流水不可变，quantity 恒为非负，方向由操作类型决定（见 doc/库存与票档预占模块.md 二）：
 * <ul>
 *   <li>INIT：初始化账本，available = total；</li>
 *   <li>RESERVE：预占，available -= q，reserved += q；</li>
 *   <li>CONFIRM：确认已售，reserved -= q，sold += q；</li>
 *   <li>RELEASE：释放预占，reserved -= q，available += q。</li>
 * </ul>
 */
public enum InventoryOperation {
    INIT,
    RESERVE,
    CONFIRM,
    RELEASE
}
