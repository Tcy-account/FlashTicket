package com.flashticket.purchase.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 抢票请求（表 ft_purchase_request）。
 *
 * <p>request_id 为一次用户抢票操作的全局幂等键，数据库唯一约束兜底防重复下单。
 * M12 起异步排队：受理时落 QUEUED（与 Outbox 事件同事务），消费者通过条件更新
 * 领取处理租约（attempt_count/lease_owner/lease_until）后建单，最终落终态。
 * 租约与恢复字段支撑「消费者崩溃后受控恢复」「消息重复投递不重复建单」。
 */
@TableName("ft_purchase_request")
public class PurchaseRequest {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String requestId;

    private Long userId;

    private Long tierId;

    private Integer quantity;

    /** 抢票时选中的用户优惠券 ID（可空，建单时正式锁券）。 */
    private Long selectedCouponId;

    private String status;

    private String failReason;

    private Long orderId;

    /** 活动 ID（受理时据 tier 确认，追踪用）。 */
    private Long eventId;

    /** 场次 ID（受理时确认）。 */
    private Long sessionId;

    /** 已开始的处理尝试次数。 */
    private Integer attemptCount;

    /** 当前处理者唯一令牌。 */
    private String leaseOwner;

    /** 处理租约到期时间（UTC）。 */
    private LocalDateTime leaseUntil;

    /** 下次允许恢复/重试时间（UTC）。 */
    private LocalDateTime nextRetryAt;

    /** 最近一次技术故障码。 */
    private String lastErrorCode;

    /** 最近一次开始处理时间（UTC）。 */
    private LocalDateTime processingStartedAt;

    /** 排队允许开始处理的截止时间（UTC）。 */
    private LocalDateTime acceptedDeadlineAt;

    /** 进入终态时间（UTC）。 */
    private LocalDateTime completedAt;

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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getSelectedCouponId() {
        return selectedCouponId;
    }

    public void setSelectedCouponId(Long selectedCouponId) {
        this.selectedCouponId = selectedCouponId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLeaseOwner() {
        return leaseOwner;
    }

    public void setLeaseOwner(String leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public LocalDateTime getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(LocalDateTime leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public LocalDateTime getAcceptedDeadlineAt() {
        return acceptedDeadlineAt;
    }

    public void setAcceptedDeadlineAt(LocalDateTime acceptedDeadlineAt) {
        this.acceptedDeadlineAt = acceptedDeadlineAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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
