package com.flashticket.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 订单明细交易快照（表 ft_order_item）。
 *
 * <p>保存成交时的活动、场次、场馆、票档名称与价格，不因后续配置修改而变
 * （见 doc/订单与交易状态机模块.md 八）。首版一笔订单仅一个票档明细，仍独立
 * 建表为多票档订单预留扩展。
 */
@TableName("ft_order_item")
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long eventId;

    private String eventTitle;

    private Long sessionId;

    private LocalDateTime sessionStartAt;

    private String venueName;

    private Long tierId;

    private String tierName;

    private Long unitPriceCent;

    private Integer quantity;

    private Long amountCent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public Long getUnitPriceCent() {
        return unitPriceCent;
    }

    public void setUnitPriceCent(Long unitPriceCent) {
        this.unitPriceCent = unitPriceCent;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
