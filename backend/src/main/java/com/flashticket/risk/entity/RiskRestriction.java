package com.flashticket.risk.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 风控临时限制/黑名单（表 ft_risk_restriction）。
 *
 * <p>subject_type + subject_id 定位主体（用户或 IP），scope 限定业务范围；
 * 到期自动解除（EXPIRED），管理员可提前解除（REVOKED），不直接禁用账号。
 */
@TableName("ft_risk_restriction")
public class RiskRestriction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String subjectType;

    private String subjectId;

    private String scope;

    private String reason;

    private LocalDateTime expireAt;

    private String status;

    private Long createdBy;

    private Long revokedBy;

    private LocalDateTime revokedAt;

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

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(Long revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
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
