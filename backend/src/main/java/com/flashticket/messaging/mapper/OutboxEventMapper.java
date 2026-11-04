package com.flashticket.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.messaging.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 事件数据访问（表 ft_outbox_event）。
 *
 * <p>发布器通过条件更新领取发布租约、标记发送结果，避免多实例重复发布同一事件。
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

    /** 扫描待发送事件（PENDING 且到重试时间），按 id 升序保证先入先出。 */
    @Select("SELECT * FROM ft_outbox_event " +
            "WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= #{now}) " +
            "ORDER BY id ASC LIMIT #{limit}")
    List<OutboxEvent> findPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 领取发布租约：PENDING → 写入租约令牌与到期时间，attempt_count + 1。 */
    @Update("UPDATE ft_outbox_event SET attempt_count = attempt_count + 1, " +
            "lease_owner = #{leaseOwner}, lease_until = #{leaseUntil} " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int acquireLease(@Param("id") Long id,
                     @Param("leaseOwner") String leaseOwner,
                     @Param("leaseUntil") LocalDateTime leaseUntil);

    /** 标记发送成功（须仍持有租约，防旧发布者错误覆盖）。 */
    @Update("UPDATE ft_outbox_event SET status = 'SENT', sent_at = #{sentAt}, " +
            "lease_owner = NULL, lease_until = NULL, last_error = NULL " +
            "WHERE id = #{id} AND status = 'PENDING' AND lease_owner = #{leaseOwner}")
    int markSent(@Param("id") Long id,
                 @Param("leaseOwner") String leaseOwner,
                 @Param("sentAt") LocalDateTime sentAt);

    /** 发送失败：释放租约并记录下次重试时间与错误摘要，保持 PENDING 待重试。 */
    @Update("UPDATE ft_outbox_event SET lease_owner = NULL, lease_until = NULL, " +
            "next_retry_at = #{nextRetryAt}, last_error = #{lastError} " +
            "WHERE id = #{id} AND status = 'PENDING' AND lease_owner = #{leaseOwner}")
    int markRetry(@Param("id") Long id,
                  @Param("leaseOwner") String leaseOwner,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("lastError") String lastError);

    /** 查询指定 topic 的失败工单（尝试过但尚未发送成功，M14 通知失败重试）。 */
    @Select("SELECT * FROM ft_outbox_event " +
            "WHERE topic = #{topic} AND status = 'PENDING' AND attempt_count > 0 " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<OutboxEvent> findFailures(@Param("topic") String topic, @Param("limit") int limit);

    /** 重置为可重发：清空重试计数与错误，交由发布器下次扫描重新领取发送。 */
    @Update("UPDATE ft_outbox_event SET attempt_count = 0, next_retry_at = NULL, " +
            "last_error = NULL, lease_owner = NULL, lease_until = NULL " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int resetForRetry(@Param("id") Long id);
}
