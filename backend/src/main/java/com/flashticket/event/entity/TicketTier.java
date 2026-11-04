package com.flashticket.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 场次票档（表 ft_ticket_tier）。
 *
 * <p>一个 Session 包含多个 TicketTier，每个票档拥有独立库存（库存账本由
 * 库存中心 M07 维护，本表仅承载票价、数量与限购配置）。票档名称同场次内
 * 唯一；version 用于乐观并发控制（见 doc/演出活动与场次.md 六）。
 */
@TableName("ft_ticket_tier")
public class TicketTier {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private String name;

    private String tierType;

    private Long unitPriceCent;

    private Integer initialQuantity;

    private Integer minPurchase;

    private Integer maxPurchase;

    private Integer perUserLimit;

    private String description;

    private Integer sortOrder;

    @Version
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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTierType() {
        return tierType;
    }

    public void setTierType(String tierType) {
        this.tierType = tierType;
    }

    public Long getUnitPriceCent() {
        return unitPriceCent;
    }

    public void setUnitPriceCent(Long unitPriceCent) {
        this.unitPriceCent = unitPriceCent;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getMinPurchase() {
        return minPurchase;
    }

    public void setMinPurchase(Integer minPurchase) {
        this.minPurchase = minPurchase;
    }

    public Integer getMaxPurchase() {
        return maxPurchase;
    }

    public void setMaxPurchase(Integer maxPurchase) {
        this.maxPurchase = maxPurchase;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
