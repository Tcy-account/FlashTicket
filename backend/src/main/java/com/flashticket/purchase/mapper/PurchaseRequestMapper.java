package com.flashticket.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.purchase.entity.PurchaseRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 抢票请求数据访问（表 ft_purchase_request）。
 *
 * <p>异步处理的核心并发控制全部走手写条件更新（不依赖 @Version 乐观锁）：
 * 领取租约用 status + lease_until 条件，提交/失败用 lease_owner 校验，保证
 * 同一请求在同一时刻只有一个持有有效租约的处理者能推进状态。
 */
@Mapper
public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {

    /** 按幂等键查询（重复提交直接返回已有结果）。 */
    @Select("SELECT * FROM ft_purchase_request WHERE request_id = #{requestId} LIMIT 1")
    PurchaseRequest findByRequestId(@Param("requestId") String requestId);

    /**
     * 领取处理租约：QUEUED/RECOVERING → PROCESSING，attempt_count + 1，
     * 写入本次处理令牌与租约到期时间。租约未到期则领取失败（返回 0）。
     */
    @Update("UPDATE ft_purchase_request SET status = 'PROCESSING', " +
            "attempt_count = attempt_count + 1, lease_owner = #{leaseOwner}, " +
            "lease_until = #{leaseUntil}, processing_started_at = #{now}, next_retry_at = NULL " +
            "WHERE request_id = #{requestId} AND status IN ('QUEUED', 'RECOVERING') " +
            "AND (lease_until IS NULL OR lease_until < #{now})")
    int acquireLease(@Param("requestId") String requestId,
                     @Param("leaseOwner") String leaseOwner,
                     @Param("leaseUntil") LocalDateTime leaseUntil,
                     @Param("now") LocalDateTime now);

    /** 标记处理成功（须仍持有有效租约，防止旧处理者错误提交）。 */
    @Update("UPDATE ft_purchase_request SET status = 'SUCCEEDED', order_id = #{orderId}, " +
            "completed_at = #{now}, lease_owner = NULL, lease_until = NULL, next_retry_at = NULL " +
            "WHERE request_id = #{requestId} AND status = 'PROCESSING' AND lease_owner = #{leaseOwner}")
    int markSucceeded(@Param("requestId") String requestId,
                      @Param("orderId") Long orderId,
                      @Param("leaseOwner") String leaseOwner,
                      @Param("now") LocalDateTime now);

    /** 标记确定性业务失败（售罄/限购等，终态，不自动重试）。 */
    @Update("UPDATE ft_purchase_request SET status = 'FAILED', fail_reason = #{failReason}, " +
            "completed_at = #{now}, lease_owner = NULL, lease_until = NULL, next_retry_at = NULL " +
            "WHERE request_id = #{requestId} AND status = 'PROCESSING' AND lease_owner = #{leaseOwner}")
    int markFailed(@Param("requestId") String requestId,
                   @Param("failReason") String failReason,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("now") LocalDateTime now);

    /** 标记可恢复（技术故障，结果不确定，等待下次恢复重试）。 */
    @Update("UPDATE ft_purchase_request SET status = 'RECOVERING', last_error_code = #{errorCode}, " +
            "next_retry_at = #{nextRetryAt}, lease_owner = NULL, lease_until = NULL " +
            "WHERE request_id = #{requestId} AND status = 'PROCESSING' AND lease_owner = #{leaseOwner}")
    int markRecovering(@Param("requestId") String requestId,
                       @Param("errorCode") String errorCode,
                       @Param("nextRetryAt") LocalDateTime nextRetryAt,
                       @Param("leaseOwner") String leaseOwner);

    /** 用户撤销尚未开始处理的排队请求（仅 QUEUED 可撤销）。 */
    @Update("UPDATE ft_purchase_request SET status = 'CANCELLED', completed_at = #{now}, " +
            "next_retry_at = NULL WHERE request_id = #{requestId} AND status = 'QUEUED'")
    int markCancelled(@Param("requestId") String requestId, @Param("now") LocalDateTime now);

    /** 扫描租约过期仍在 PROCESSING 的请求（消费者崩溃，需恢复）。 */
    @Select("SELECT * FROM ft_purchase_request " +
            "WHERE status = 'PROCESSING' AND lease_until < #{now} " +
            "ORDER BY id ASC LIMIT #{limit}")
    List<PurchaseRequest> findLeaseExpired(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 扫描可恢复请求（RECOVERING 且到重试时间）。 */
    @Select("SELECT * FROM ft_purchase_request " +
            "WHERE status = 'RECOVERING' AND next_retry_at <= #{now} " +
            "ORDER BY id ASC LIMIT #{limit}")
    List<PurchaseRequest> findRecoverable(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 扫描排队超时请求（QUEUED 且超过允许开始处理截止时间）。 */
    @Select("SELECT * FROM ft_purchase_request " +
            "WHERE status = 'QUEUED' AND accepted_deadline_at IS NOT NULL " +
            "AND accepted_deadline_at < #{now} ORDER BY id ASC LIMIT #{limit}")
    List<PurchaseRequest> findDeadlineExpired(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 我的抢票历史分页（按创建时间倒序）。 */
    @Select("SELECT * FROM ft_purchase_request WHERE user_id = #{userId} ORDER BY id DESC")
    IPage<PurchaseRequest> selectUserPage(Page<PurchaseRequest> page, @Param("userId") Long userId);

    /** 管理端抢票请求分页（可按状态筛选）。 */
    @Select("<script>SELECT * FROM ft_purchase_request " +
            "<where><if test='status != null'> AND status = #{status}</if></where> " +
            "ORDER BY id DESC</script>")
    IPage<PurchaseRequest> selectAdminPage(Page<PurchaseRequest> page, @Param("status") String status);

    /** 复位可重试请求（管理端重试）：清空重试时间与故障码，允许立即重新领取租约。 */
    @Update("UPDATE ft_purchase_request SET next_retry_at = NULL, last_error_code = NULL " +
            "WHERE request_id = #{requestId} AND status = 'RECOVERING'")
    int markRetryable(@Param("requestId") String requestId);

    /** 租约过期复位：PROCESSING 且租约到期 → RECOVERING，清空租约，允许立即恢复。 */
    @Update("UPDATE ft_purchase_request SET status = 'RECOVERING', next_retry_at = #{now}, " +
            "lease_owner = NULL, lease_until = NULL, last_error_code = 'LEASE_EXPIRED' " +
            "WHERE request_id = #{requestId} AND status = 'PROCESSING' AND lease_until < #{now}")
    int resetLeaseExpired(@Param("requestId") String requestId, @Param("now") LocalDateTime now);

    /** 领取恢复权：RECOVERING 且到重试时间 → 把重试时间后移（防多实例重复补发）。 */
    @Update("UPDATE ft_purchase_request SET next_retry_at = #{nextRetryAt} " +
            "WHERE request_id = #{requestId} AND status = 'RECOVERING' AND next_retry_at <= #{now}")
    int claimRecovery(@Param("requestId") String requestId,
                      @Param("now") LocalDateTime now,
                      @Param("nextRetryAt") LocalDateTime nextRetryAt);

    /** 排队超时终态：QUEUED 且超过受理截止时间 → FAILED（原因 QUEUE_TIMEOUT）。 */
    @Update("UPDATE ft_purchase_request SET status = 'FAILED', fail_reason = 'QUEUE_TIMEOUT', " +
            "completed_at = #{now}, next_retry_at = NULL " +
            "WHERE request_id = #{requestId} AND status = 'QUEUED' " +
            "AND accepted_deadline_at IS NOT NULL AND accepted_deadline_at < #{now}")
    int markQueueTimeout(@Param("requestId") String requestId, @Param("now") LocalDateTime now);
}
