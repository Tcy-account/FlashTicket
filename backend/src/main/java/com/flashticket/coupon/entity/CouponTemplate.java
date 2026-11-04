package com.flashticket.coupon.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 优惠券模板与规则（表 ft_coupon_template）。
 */
@TableName("ft_coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long campaignId;

    private String name;

    private String type;

    private Long thresholdCent;

    private Long discountCent;

    private Integer discountRate;

    private Long maxDiscountCent;

    private Integer totalQuantity;

    private Integer perUserLimit;

    private LocalDateTime claimStartAt;

    private LocalDateTime claimEndAt;

    private String validityType;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private Integer validDays;

    private String applicableScope;

    private Long scopeRefId;

    private String status;

    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getThresholdCent() {
        return thresholdCent;
    }

    public void setThresholdCent(Long thresholdCent) {
        this.thresholdCent = thresholdCent;
    }

    public Long getDiscountCent() {
        return discountCent;
    }

    public void setDiscountCent(Long discountCent) {
        this.discountCent = discountCent;
    }

    public Integer getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Integer discountRate) {
        this.discountRate = discountRate;
    }

    public Long getMaxDiscountCent() {
        return maxDiscountCent;
    }

    public void setMaxDiscountCent(Long maxDiscountCent) {
        this.maxDiscountCent = maxDiscountCent;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public LocalDateTime getClaimStartAt() {
        return claimStartAt;
    }

    public void setClaimStartAt(LocalDateTime claimStartAt) {
        this.claimStartAt = claimStartAt;
    }

    public LocalDateTime getClaimEndAt() {
        return claimEndAt;
    }

    public void setClaimEndAt(LocalDateTime claimEndAt) {
        this.claimEndAt = claimEndAt;
    }

    public String getValidityType() {
        return validityType;
    }

    public void setValidityType(String validityType) {
        this.validityType = validityType;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Integer getValidDays() {
        return validDays;
    }

    public void setValidDays(Integer validDays) {
        this.validDays = validDays;
    }

    public String getApplicableScope() {
        return applicableScope;
    }

    public void setApplicableScope(String applicableScope) {
        this.applicableScope = applicableScope;
    }

    public Long getScopeRefId() {
        return scopeRefId;
    }

    public void setScopeRefId(Long scopeRefId) {
        this.scopeRefId = scopeRefId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
