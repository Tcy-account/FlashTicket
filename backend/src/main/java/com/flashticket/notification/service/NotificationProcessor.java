package com.flashticket.notification.service;

import com.flashticket.messaging.entity.MessageInbox;
import com.flashticket.messaging.mapper.MessageInboxMapper;
import com.flashticket.notification.dto.NotificationEvent;
import com.flashticket.notification.dto.NotificationResponse;
import com.flashticket.notification.entity.Notification;
import com.flashticket.notification.entity.NotificationRecipient;
import com.flashticket.notification.enums.NotificationEventType;
import com.flashticket.notification.mapper.NotificationMapper;
import com.flashticket.notification.mapper.NotificationRecipientMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 通知消费处理（M14 P0-C，无 MQ 依赖，集成测试可直接调用）。
 *
 * <p>处理一条通知业务事件：Inbox 幂等 → 按事件类型渲染标题正文 → 落库
 * （通知内容 + 接收记录 + Inbox 同一事务）→ 事务提交后 SSE 推送。RocketMQ
 * 至少一次投递由 Inbox (consumer_name, event_id) 唯一约束兜底，重复消费不重复通知。
 *
 * <p>消费返回语义（listener 据此决定 MQ 重试）：
 * <ul>
 *   <li>业务确定结果（成功/幂等跳过/未知事件类型）→ 消费完成，不重试；</li>
 *   <li>技术故障（DB 不可用等）→ 异常传播，由 MQ 重试。</li>
 * </ul>
 */
@Service
public class NotificationProcessor {

    /** 消费组名，同时作为 Inbox 的 consumer_name 去重维度。 */
    public static final String CONSUMER_NAME = "ft-notification-cg";

    private final MessageInboxMapper inboxMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationRecipientMapper recipientMapper;
    private final SseConnectionRegistry sseRegistry;

    public NotificationProcessor(MessageInboxMapper inboxMapper,
                                 NotificationMapper notificationMapper,
                                 NotificationRecipientMapper recipientMapper,
                                 SseConnectionRegistry sseRegistry) {
        this.inboxMapper = inboxMapper;
        this.notificationMapper = notificationMapper;
        this.recipientMapper = recipientMapper;
        this.sseRegistry = sseRegistry;
    }

    /**
     * 处理通知事件。
     *
     * @return true 表示已确定处理完成（成功/幂等跳过/未知类型）；异常传播表示
     *         技术故障，由 MQ 重试。
     */
    @Transactional
    public boolean process(NotificationEvent event) {
        // 1. Inbox 幂等：同一事件已消费，直接跳过
        if (inboxMapper.countByConsumerAndEvent(CONSUMER_NAME, event.eventId()) > 0) {
            return true;
        }

        // 2. 解析事件类型（未知类型为确定性失败：记录 Inbox 防无限重试）
        NotificationEventType type;
        try {
            type = NotificationEventType.valueOf(event.eventType());
        } catch (IllegalArgumentException e) {
            recordInbox(event.eventId());
            return true;
        }

        // 3. 落库：通知内容 + 接收记录 + Inbox（同一事务，唯一约束兜底并发重复）
        Notification notification = new Notification();
        notification.setType(type.type().name());
        notification.setTitle(type.title());
        notification.setContent(type.content());
        notification.setBizType(type.bizType());
        notification.setBizId(event.bizId());
        notification.setEventId(event.eventId());
        notificationMapper.insert(notification);

        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setNotificationId(notification.getId());
        recipient.setUserId(event.userId());
        recipientMapper.insert(recipient);

        recordInbox(event.eventId());

        // 4. 事务提交后 SSE 推送（推送失败不回滚通知，离线用户 REST 补查）
        NotificationResponse response = toResponse(recipient, notification);
        afterCommit(() -> {
            sseRegistry.pushToUser(event.userId(), "notification.created", response);
            sseRegistry.pushToUser(event.userId(), "notification.unread-count",
                    recipientMapper.countUnread(event.userId()));
        });

        return true;
    }

    /* ---------- 内部 ---------- */

    /** 记录消费幂等（并发重复由唯一约束兜底，冲突向上抛出使事务回滚）。 */
    private void recordInbox(String eventId) {
        MessageInbox inbox = new MessageInbox();
        inbox.setConsumerName(CONSUMER_NAME);
        inbox.setEventId(eventId);
        inbox.setProcessedAt(LocalDateTime.now(ZoneOffset.UTC));
        inboxMapper.insert(inbox);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private NotificationResponse toResponse(NotificationRecipient r, Notification n) {
        return new NotificationResponse(
                String.valueOf(r.getId()), n.getType(), n.getTitle(), n.getContent(),
                n.getBizType(), n.getBizId(), false, r.getCreatedAt());
    }
}
