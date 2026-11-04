package com.flashticket.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.flashticket.order.enums.OrderOperationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单操作流水（表 ft_order_operation），记录订单状态变更历史（业务审计）。
 *
 * <p>operation_id 唯一，作为幂等键防止重复记录；from_status 为 null 表示创建
 * 事件。actor_id 为用户取消时的操作主体，系统关单为 null。
 */
@TableName("ft_order_operation")
public class OrderOperation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String operation;

    private String fromStatus;

    private String toStatus;

    private Long actorId;

    private String reason;

    private String operationId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 构造一条订单操作流水（operation_id 用 UUID，唯一幂等）。
     *
     * @param orderId    订单 ID
     * @param type       操作类型
     * @param fromStatus 操作前状态（创建事件传 null）
     * @param toStatus   操作后状态
     * @param actorId    操作主体（系统操作传 null）
     * @param reason     关单原因（非关单事件传 null）
     */
    public static OrderOperation of(Long orderId, OrderOperationType type,
                                    String fromStatus, String toStatus,
                                    Long actorId, String reason) {
        OrderOperation op = new OrderOperation();
        op.setOrderId(orderId);
        op.setOperation(type.name());
        op.setFromStatus(fromStatus);
        op.setToStatus(toStatus);
        op.setActorId(actorId);
        op.setReason(reason);
        op.setOperationId(UUID.randomUUID().toString());
        return op;
    }

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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
