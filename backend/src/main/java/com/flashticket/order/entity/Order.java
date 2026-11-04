package com.flashticket.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 订单主体（表 ft_order）。
 *
 * <p>order_no 用户可见业务号唯一，request_id 唯一保证一个抢票请求最多生成一笔订单。
 * 金额字段为交易快照，创建后不因票价配置变更而自动更新（见跨域审查 C07）。
 * 状态机 PENDING_PAYMENT → PAID / CLOSED，关闭原因见 close_reason（M09 落地）。
 */
@TableName("ft_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private String requestId;

    private Long userId;

    private String status;

    private Long originalAmountCent;

    private Long discountAmountCent;

    /** 使用的用户优惠券 ID（M13，可空）。 */
    private Long userCouponId;

    /** 优惠券模板 ID（M13，可空）。 */
    private Long couponTemplateId;

    /** 优惠规则快照 JSON（M13，可空）。 */
    private String promotionSnapshot;

    private Long payableAmountCent;

    private Long paidAmountCent;

    private LocalDateTime expireAt;

    private LocalDateTime paidAt;

    private LocalDateTime closedAt;

    private String closeReason;

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

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getOriginalAmountCent() {
        return originalAmountCent;
    }

    public void setOriginalAmountCent(Long originalAmountCent) {
        this.originalAmountCent = originalAmountCent;
    }

    public Long getDiscountAmountCent() {
        return discountAmountCent;
    }

    public void setDiscountAmountCent(Long discountAmountCent) {
        this.discountAmountCent = discountAmountCent;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }

    public Long getCouponTemplateId() {
        return couponTemplateId;
    }

    public void setCouponTemplateId(Long couponTemplateId) {
        this.couponTemplateId = couponTemplateId;
    }

    public String getPromotionSnapshot() {
        return promotionSnapshot;
    }

    public void setPromotionSnapshot(String promotionSnapshot) {
        this.promotionSnapshot = promotionSnapshot;
    }

    public Long getPayableAmountCent() {
        return payableAmountCent;
    }

    public void setPayableAmountCent(Long payableAmountCent) {
        this.payableAmountCent = payableAmountCent;
    }

    public Long getPaidAmountCent() {
        return paidAmountCent;
    }

    public void setPaidAmountCent(Long paidAmountCent) {
        this.paidAmountCent = paidAmountCent;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
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
