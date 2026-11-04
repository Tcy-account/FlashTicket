package com.flashticket.inventory.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.inventory.dto.InventoryLedgerResponse;
import com.flashticket.inventory.dto.InventoryResponse;
import com.flashticket.inventory.dto.ReservationResponse;
import com.flashticket.inventory.service.InventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台库存查询入口（M07），只读 inventory:read 权限。
 *
 * <p>预占/确认/释放为库存领域服务，供抢票与订单编排层内部调用，不直接对前端
 * 开放（见 doc/库存与票档预占模块.md 十一）。库存调整/冻结/对账等高风险写操作
 * 留待 M16 落地对应权限点（inventory:adjust/manage/reconcile）。
 */
@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** 库存总账查询。 */
    @GetMapping("/{tierId}")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<InventoryResponse> get(@PathVariable Long tierId) {
        return ApiResponse.ok(inventoryService.getInventory(tierId));
    }

    /** 某票档的全部预占记录。 */
    @GetMapping("/{tierId}/reservations")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<List<ReservationResponse>> reservations(@PathVariable Long tierId) {
        return ApiResponse.ok(inventoryService.listReservations(tierId));
    }

    /** 某票档的库存变化流水。 */
    @GetMapping("/{tierId}/ledger")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<List<InventoryLedgerResponse>> ledger(@PathVariable Long tierId) {
        return ApiResponse.ok(inventoryService.listLedger(tierId));
    }
}
