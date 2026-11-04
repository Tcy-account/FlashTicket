package com.flashticket.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 售票计划（表 ft_sale_plan），一对一绑定票档。
 *
 * <p>售票计划负责「何时卖、是否暂停」（见 doc/演出活动与场次.md 七），与
 * 抢票中心区分：售票计划定义能不能买，抢票中心负责高并发下能不能抢到。
 * sale_version 每次开售/暂停/恢复/时间变更递增，供后续库存/抢票模块校验
 * 过期配置，避免 Redis 旧缓存放行请求。
 */
@TableName("ft_sale_plan")
public class SalePlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tierId;

    private LocalDateTime saleStartAt;

    private LocalDateTime saleEndAt;

    private String saleStatus;

    private Long saleVersion;

    private LocalDateTime pausedAt;

    private String pauseReason;

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

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public LocalDateTime getSaleStartAt() {
        return saleStartAt;
    }

    public void setSaleStartAt(LocalDateTime saleStartAt) {
        this.saleStartAt = saleStartAt;
    }

    public LocalDateTime getSaleEndAt() {
        return saleEndAt;
    }

    public void setSaleEndAt(LocalDateTime saleEndAt) {
        this.saleEndAt = saleEndAt;
    }

    public String getSaleStatus() {
        return saleStatus;
    }

    public void setSaleStatus(String saleStatus) {
        this.saleStatus = saleStatus;
    }

    public Long getSaleVersion() {
        return saleVersion;
    }

    public void setSaleVersion(Long saleVersion) {
        this.saleVersion = saleVersion;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
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
