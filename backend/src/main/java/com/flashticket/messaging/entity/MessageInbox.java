package com.flashticket.messaging.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 消息消费幂等记录（表 ft_message_inbox）。
 *
 * <p>RocketMQ 至少一次投递，同一事件可能被重复消费。Inbox 以
 * (consumer_name, event_id) 唯一约束去重：消费处理与 Inbox 写入同事务提交，
 * 避免「消息已标记消费但业务未完成」。见 doc/全局数据库模型与ER关系设计.md 十四。
 */
@TableName("ft_message_inbox")
public class MessageInbox {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String consumerName;

    private String eventId;

    private LocalDateTime processedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
