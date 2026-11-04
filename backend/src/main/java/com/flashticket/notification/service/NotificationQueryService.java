package com.flashticket.notification.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.notification.dto.NotificationListResult;
import com.flashticket.notification.dto.NotificationResponse;
import com.flashticket.notification.dto.NotificationView;
import com.flashticket.notification.entity.Notification;
import com.flashticket.notification.entity.NotificationRecipient;
import com.flashticket.notification.mapper.NotificationMapper;
import com.flashticket.notification.mapper.NotificationRecipientMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 用户消息查询与已读管理（M14 P0-A）。
 *
 * <p>消息列表用游标分页（接收记录 id 倒序），已读操作用条件更新判成败，
 * 保证归属隔离：只能读/改本人的接收记录，无法越权或重复标记已读。
 */
@Service
public class NotificationQueryService {

    private final NotificationRecipientMapper recipientMapper;
    private final NotificationMapper notificationMapper;

    public NotificationQueryService(NotificationRecipientMapper recipientMapper,
                                    NotificationMapper notificationMapper) {
        this.recipientMapper = recipientMapper;
        this.notificationMapper = notificationMapper;
    }

    /** 我的消息游标分页（type 可空=全部）。 */
    public NotificationListResult listMine(Long userId, String type, Long cursor, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        List<NotificationView> views = recipientMapper.selectPageByUser(
                userId, trimToNull(type), cursor, size);

        List<NotificationResponse> items = views.stream()
                .map(this::toResponse).toList();
        String nextCursor = null;
        if (views.size() == size) {
            nextCursor = String.valueOf(views.get(views.size() - 1).getRecipientId());
        }
        return new NotificationListResult(items, nextCursor,
                recipientMapper.countUnread(userId));
    }

    /** 消息详情（归属校验）。 */
    public NotificationResponse getDetail(Long userId, Long recipientId) {
        NotificationRecipient recipient = requireRecipient(recipientId);
        if (!recipient.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOTIFICATION_NOT_OWNED);
        }
        Notification notification = notificationMapper.selectById(recipient.getNotificationId());
        if (notification == null) {
            throw new BizException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return toResponse(recipient, notification);
    }

    /** 未读数。 */
    public long unreadCount(Long userId) {
        return recipientMapper.countUnread(userId);
    }

    /** 单条已读（幂等：已读返回成功不报错）。 */
    @Transactional
    public void markRead(Long userId, Long recipientId) {
        NotificationRecipient recipient = requireRecipient(recipientId);
        if (!recipient.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOTIFICATION_NOT_OWNED);
        }
        recipientMapper.markRead(recipientId, userId, LocalDateTime.now(ZoneOffset.UTC));
    }

    /** 全部已读。 */
    @Transactional
    public void markAllRead(Long userId) {
        recipientMapper.markAllRead(userId, LocalDateTime.now(ZoneOffset.UTC));
    }

    /* ---------- 内部 ---------- */

    private NotificationRecipient requireRecipient(Long recipientId) {
        NotificationRecipient recipient = recipientMapper.selectById(recipientId);
        if (recipient == null) {
            throw new BizException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return recipient;
    }

    private NotificationResponse toResponse(NotificationView v) {
        return new NotificationResponse(
                String.valueOf(v.getRecipientId()), v.getType(), v.getTitle(), v.getContent(),
                v.getBizType(), v.getBizId(), v.getReadAt() != null, v.getReceivedAt());
    }

    private NotificationResponse toResponse(NotificationRecipient r, Notification n) {
        return new NotificationResponse(
                String.valueOf(r.getId()), n.getType(), n.getTitle(), n.getContent(),
                n.getBizType(), n.getBizId(), r.getReadAt() != null, r.getCreatedAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
