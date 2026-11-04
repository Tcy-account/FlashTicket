package com.flashticket.inventory.dto;

import java.time.LocalDateTime;

/**
 * 库存总账视图（表 ft_inventory）。
 *
 * <p>三态账本：total = available + reserved + sold（见 doc/库存与票档预占模块.md 二）。
 * 所有雪花 ID 以字符串传输，避免 JS 精度丢失。
 */
public record InventoryResponse(
        String tierId,
        Integer total,
        Integer available,
        Integer reserved,
        Integer sold,
        Long version,
        Long generation,
        LocalDateTime updatedAt) {
}
