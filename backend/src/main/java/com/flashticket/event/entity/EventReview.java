package com.flashticket.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 活动审核记录（表 ft_event_review）。
 *
 * <p>跨域审查 C05：采用整体审核配置指纹（snapshot_hash），而非仅检查
 * Event 行版本——活动主表版本不变时，运营可能已改动某票档售价。提交审核
 * 时规范化生成审核范围配置摘要，审核通过至发布前保持冻结（见
 * doc/演出活动与场次.md 3.4）。
 */
@TableName("ft_event_review")
public class EventReview {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long eventId;

    private Long submittedBy;

    private Long eventVersion;

    private String snapshotHash;

    private Integer snapshotSchemaVersion;

    private String reviewStatus;

    private Long reviewerId;

    private String reviewComment;

    private LocalDateTime reviewedAt;

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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Long getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Long eventVersion) {
        this.eventVersion = eventVersion;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    public Integer getSnapshotSchemaVersion() {
        return snapshotSchemaVersion;
    }

    public void setSnapshotSchemaVersion(Integer snapshotSchemaVersion) {
        this.snapshotSchemaVersion = snapshotSchemaVersion;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
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
