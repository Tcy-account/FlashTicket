package com.flashticket.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 演出场次（表 ft_session）。
 *
 * <p>一个 Event 包含多个 Session，一条 Session 只属于一个 Event；
 * 同一场馆同时段不可排期冲突。version 用于乐观并发控制
 * （见 doc/全局逻辑ER模型.md 五）。
 */
@TableName("ft_session")
public class Session {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long eventId;

    private Long venueId;

    private String name;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime entryStartAt;

    private Integer saleCapacity;

    private String status;

    private LocalDateTime postponedAt;

    private LocalDateTime cancelledAt;

    private String changeReason;

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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public LocalDateTime getEntryStartAt() {
        return entryStartAt;
    }

    public void setEntryStartAt(LocalDateTime entryStartAt) {
        this.entryStartAt = entryStartAt;
    }

    public Integer getSaleCapacity() {
        return saleCapacity;
    }

    public void setSaleCapacity(Integer saleCapacity) {
        this.saleCapacity = saleCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPostponedAt() {
        return postponedAt;
    }

    public void setPostponedAt(LocalDateTime postponedAt) {
        this.postponedAt = postponedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
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
