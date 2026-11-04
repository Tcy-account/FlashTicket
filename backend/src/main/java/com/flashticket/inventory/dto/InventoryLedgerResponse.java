package com.flashticket.inventory.dto;

import java.time.LocalDateTime;

/**
 * 库存变化流水视图（表 ft_inventory_ledger），不可变账本。
 */
public record InventoryLedgerResponse(
        String id,
        String operationId,
        String tierId,
        String reservationId,
        String operation,
        Integer quantity,
        LocalDateTime createdAt) {
}
