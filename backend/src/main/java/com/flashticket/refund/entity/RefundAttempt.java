package com.flashticket.refund.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 退款执行尝试（表 ft_refund_attempt）。
 *
 * <p>解决「退款请求已提交但处理中应用崩溃，结果不确定」问题：每次执行保留一条
 * 确定结果或不确定状态的尝试。(refund_id, attempt_no)、channel_request_no 唯一，
 * 见 doc/全局逻辑ER模型.md 七。
 */
@TableName("ft_refund_attempt")
public class RefundAttempt {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long refundId;

    private Integer attemptNo;

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

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
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
