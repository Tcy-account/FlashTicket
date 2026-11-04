package com.flashticket.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 模拟支付回调（表 ft_payment_callback）。
 *
 * <p>(channel, channel_event_id) 唯一，用于模拟渠道重复通知去重，防止重复
 * 确认支付、重复扣款或重复核销（见 doc/模拟支付与退款模块.md 五）。
 */
@TableName("ft_payment_callback")
public class PaymentCallback {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long paymentId;

    private Long attemptId;

    private String channel;

    private String channelEventId;

    private String status;

    private String resultCode;

    private LocalDateTime receivedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelEventId() {
        return channelEventId;
    }

    public void setChannelEventId(String channelEventId) {
        this.channelEventId = channelEventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
