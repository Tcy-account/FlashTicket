package com.flashticket.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 支付执行尝试（表 ft_payment_attempt）。
 *
 * <p>一次支付业务可多次尝试；(payment_id, attempt_no)、attempt_request_id、
 * channel_request_no 三处唯一，分别保证尝试序号、HTTP 幂等、渠道幂等
 * （见 doc/全局逻辑ER模型.md 四·支付执行尝试表）。
 */
@TableName("ft_payment_attempt")
public class PaymentAttempt {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long paymentId;

    private Integer attemptNo;

    private String attemptRequestId;

    private String channelRequestNo;

    private String status;

    private Long amountCent;

    private String resultCode;

    private String resultMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

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

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getAttemptRequestId() {
        return attemptRequestId;
    }

    public void setAttemptRequestId(String attemptRequestId) {
        this.attemptRequestId = attemptRequestId;
    }

    public String getChannelRequestNo() {
        return channelRequestNo;
    }

    public void setChannelRequestNo(String channelRequestNo) {
        this.channelRequestNo = channelRequestNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
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
