package com.flashticket.inventory.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 库存总账（表 ft_inventory），一票档一账本。
 *
 * <p>三态守恒：total = available + reserved + sold（数据库 CHECK 兜底）。
 * 账目变更一律走手写条件更新（available >= q 之类），version 为普通字段在
 * SQL 内自增，不使用 {@code @Version} 乐观锁插件（条件更新已自带并发防护）。
 *
 * <p>主键即票档 ID（tier_id），由发布时初始化写入，非雪花自增。
 */
@TableName("ft_inventory")
public class Inventory {

    @TableId(value = "tier_id", type = IdType.INPUT)
    private Long tierId;

    private Integer total;

    private Integer available;

    private Integer reserved;

    private Integer sold;

    private Long version;

    private Long generation;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }

    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getGeneration() {
        return generation;
    }

    public void setGeneration(Long generation) {
        this.generation = generation;
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
