package com.flashticket.inventory.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户限购账本（表 ft_purchase_limit），UNIQUE(user_id, tier_id)。
 *
 * <p>限购额度 = reserved_quantity（未支付预占）+ sold_quantity（仍计入限购的
 * 已支付票数），校验上限来自票档 per_user_limit。预占/确认/释放在此账本上
 * 原子占用/转移，退款不自动恢复限购名额（见 doc/全局数据库模型与ER关系设计.md 七）。
 */
@TableName("ft_purchase_limit")
public class PurchaseLimit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long tierId;

    private Integer reservedQuantity;

    private Integer soldQuantity;

    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
