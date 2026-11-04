package com.flashticket.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 支付业务单（表 ft_payment）。
 *
 * <p>一个订单在 V1 中只对应一个支付业务单（order_id 唯一），金额取自订单
 * 应付金额快照，用户不能通过请求篡改。状态机 CREATED → PROCESSING →
 * SUCCEEDED / FAILED / CLOSED（见 doc/模拟支付与退款模块.md 四）。
 */
@TableName("ft_payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private String channel;

    private Long amountCent;

    private String currency;

    private String status;

    private LocalDateTime expireAt;

    private Long currentAttemptId;

    private Long successfulAttemptId;

    private LocalDateTime processingStartedAt;

    private LocalDateTime succeededAt;

    private LocalDateTime failedAt;

    private LocalDateTime closedAt;

    private String failureCode;

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

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public Long getCurrentAttemptId() {
        return currentAttemptId;
    }

    public void setCurrentAttemptId(Long currentAttemptId) {
        this.currentAttemptId = currentAttemptId;
    }

    public Long getSuccessfulAttemptId() {
        return successfulAttemptId;
    }

    public void setSuccessfulAttemptId(Long successfulAttemptId) {
        this.successfulAttemptId = successfulAttemptId;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public LocalDateTime getSucceededAt() {
        return succeededAt;
    }

    public void setSucceededAt(LocalDateTime succeededAt) {
        this.succeededAt = succeededAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
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
