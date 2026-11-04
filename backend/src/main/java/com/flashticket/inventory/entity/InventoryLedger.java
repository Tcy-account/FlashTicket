package com.flashticket.inventory.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 库存变化流水（表 ft_inventory_ledger），不可变账本。
 *
 * <p>operation_id 唯一，每次库存操作写入一条；quantity 非负，方向由 operation
 * 决定。流水承担故障审计与库存重建依据，禁止物理删除或改写。
 */
@TableName("ft_inventory_ledger")
public class InventoryLedger {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String operationId;

    private Long tierId;

    private Long reservationId;

    private String operation;

    private Integer quantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
