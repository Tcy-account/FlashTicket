package com.flashticket.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.notification.dto.NotificationView;
import com.flashticket.notification.entity.NotificationRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知接收记录数据访问（表 ft_notification_recipient）。
 *
 * <p>游标分页以接收记录 id（雪花、单调递增）为游标，join 通知内容一次取回；
 * 已读操作用条件更新（read_at IS NULL）判成败，保证「未读→已读」单次生效、
 * 不可重复或越权修改他人记录。
 */
@Mapper
public interface NotificationRecipientMapper extends BaseMapper<NotificationRecipient> {

    /**
     * 用户消息游标分页（按接收记录 id 倒序）。
     *
     * @param cursor 上一页最后一条接收记录 id（null 表示第一页）
     * @param type   通知类型过滤（null 表示全部）
     */
    @Select("""
            <script>
            SELECT r.id AS recipient_id,
                   r.notification_id AS notification_id,
                   r.read_at AS read_at,
                   r.created_at AS received_at,
                   n.type AS type,
                   n.title AS title,
                   n.content AS content,
                   n.biz_type AS biz_type,
                   n.biz_id AS biz_id
              FROM ft_notification_recipient r
              JOIN ft_notification n ON n.id = r.notification_id
             WHERE r.user_id = #{userId}
               AND r.hidden_at IS NULL
               <if test="cursor != null">AND r.id &lt; #{cursor}</if>
               <if test="type != null">AND n.type = #{type}</if>
             ORDER BY r.id DESC
             LIMIT #{limit}
            </script>
            """)
    List<NotificationView> selectPageByUser(@Param("userId") Long userId,
                                            @Param("type") String type,
                                            @Param("cursor") Long cursor,
                                            @Param("limit") int limit);

    /** 单条已读：仅未读→已读且归属本人，返回受影响行数（1 成功，0 已读/非本人）。 */
    @Update("""
            UPDATE ft_notification_recipient
               SET read_at = #{now}
             WHERE id = #{recipientId} AND user_id = #{userId} AND read_at IS NULL
            """)
    int markRead(@Param("recipientId") Long recipientId,
                 @Param("userId") Long userId,
                 @Param("now") LocalDateTime now);

    /** 全部已读：当前用户所有未读一次性标记，返回受影响行数。 */
    @Update("""
            UPDATE ft_notification_recipient
               SET read_at = #{now}
             WHERE user_id = #{userId} AND read_at IS NULL
            """)
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 未读计数（hidden 过滤为 P1 软删预留）。 */
    @Select("""
            SELECT COUNT(*) FROM ft_notification_recipient
             WHERE user_id = #{userId} AND read_at IS NULL AND hidden_at IS NULL
            """)
    int countUnread(@Param("userId") Long userId);

    /** 按通知 + 用户定位接收记录（归属校验）。 */
    @Select("""
            SELECT * FROM ft_notification_recipient
             WHERE notification_id = #{notificationId} AND user_id = #{userId}
            """)
    NotificationRecipient findByNotificationAndUser(@Param("notificationId") Long notificationId,
                                                    @Param("userId") Long userId);
}
