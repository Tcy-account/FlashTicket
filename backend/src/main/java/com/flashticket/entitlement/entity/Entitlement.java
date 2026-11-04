package com.flashticket.entitlement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 票务权益（表 ft_ticket_entitlement）。
 *
 * <p>一票一权益：支付成功时为订单明细的每个购买数量生成一条独立权益记录，
 * 记录成交时的活动/场次/场馆/票档快照。(order_item_id, ticket_index) 唯一
 * 保证重复出票幂等；entitlement_no 为面向用户的凭证号。
 */
@TableName("ft_ticket_entitlement")
public class Entitlement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String entitlementNo;

    private Long orderId;

    private Long orderItemId;

    private Long userId;

    private Long eventId;

    private String eventTitle;

    private Long sessionId;

    private LocalDateTime sessionStartAt;

    private String venueName;

    private Long tierId;

    private String tierName;

    private Integer ticketIndex;

    private String status;

    private Long version;

    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

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

    public String getEntitlementNo() {
        return entitlementNo;
    }

    public void setEntitlementNo(String entitlementNo) {
        this.entitlementNo = entitlementNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getSessionStartAt() {
        return sessionStartAt;
    }

    public void setSessionStartAt(LocalDateTime sessionStartAt) {
        this.sessionStartAt = sessionStartAt;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Integer getTicketIndex() {
        return ticketIndex;
    }

    public void setTicketIndex(Integer ticketIndex) {
        this.ticketIndex = ticketIndex;
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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
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
