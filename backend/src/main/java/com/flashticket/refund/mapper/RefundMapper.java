package com.flashticket.refund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.refund.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 退款业务单数据访问（表 ft_refund）。
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /** 按订单查询退款业务单（V1 一订单一退款业务，不存在返回 null）。 */
    @Select("SELECT * FROM ft_refund WHERE order_id = #{orderId} LIMIT 1")
    Refund findByOrderId(@Param("orderId") Long orderId);

    /** 按申请幂等号查询退款业务单（不存在返回 null）。 */
    @Select("SELECT * FROM ft_refund WHERE refund_request_id = #{requestId} LIMIT 1")
    Refund findByRefundRequestId(@Param("requestId") String requestId);

    /**
     * 条件审核通过：仅 PENDING_REVIEW → APPROVED。
     *
     * <p>返回受影响行数（1 表示本次审核生效，0 表示已被并发审核/撤销）。
     */
    @Update("""
            UPDATE ft_refund
               SET status = 'APPROVED', approved_by = #{approvedBy}, approved_at = #{approvedAt},
                   review_comment = #{comment}, version = version + 1, updated_at = #{approvedAt}
             WHERE id = #{id} AND status = 'PENDING_REVIEW'
            """)
    int markApproved(@Param("id") Long id,
                     @Param("approvedBy") Long approvedBy,
                     @Param("approvedAt") LocalDateTime approvedAt,
                     @Param("comment") String comment);

    /**
     * 条件审核拒绝：仅 PENDING_REVIEW → REJECTED。
     */
    @Update("""
            UPDATE ft_refund
               SET status = 'REJECTED', rejected_by = #{rejectedBy}, rejected_at = #{rejectedAt},
                   review_comment = #{comment}, version = version + 1, updated_at = #{rejectedAt}
             WHERE id = #{id} AND status = 'PENDING_REVIEW'
            """)
    int markRejected(@Param("id") Long id,
                     @Param("rejectedBy") Long rejectedBy,
                     @Param("rejectedAt") LocalDateTime rejectedAt,
                     @Param("comment") String comment);

    /**
     * 条件撤销：仅 PENDING_REVIEW → CANCELLED（用户撤销未审核申请）。
     */
    @Update("""
            UPDATE ft_refund
               SET status = 'CANCELLED', version = version + 1, updated_at = #{now}
             WHERE id = #{id} AND status = 'PENDING_REVIEW'
            """)
    int markCancelled(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 条件退款成功：仅 APPROVED / FAILED → SUCCEEDED。
     *
     * <p>APPROVED 为审核通过后的自动执行入口，FAILED 为财务重试入口。返回受影响
     * 行数（1 表示本次确认生效，0 表示已被并发确认，幂等）。
     */
    @Update("""
            UPDATE ft_refund
               SET status = 'SUCCEEDED', succeeded_at = #{succeededAt},
                   current_attempt_id = #{attemptId}, version = version + 1,
                   updated_at = #{succeededAt}
             WHERE id = #{id} AND status IN ('APPROVED', 'FAILED')
            """)
    int markSucceeded(@Param("id") Long id,
                      @Param("attemptId") Long attemptId,
                      @Param("succeededAt") LocalDateTime succeededAt);

    /** 我的退款分页（按 id 倒序，可按状态筛选）。 */
    @Select("""
            <script>
            SELECT * FROM ft_refund WHERE user_id = #{userId}
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Refund> selectUserPage(Page<Refund> page,
                                  @Param("userId") Long userId,
                                  @Param("status") String status);

    /** 管理端退款分页（按 id 倒序，可按状态/退款号筛选）。 */
    @Select("""
            <script>
            SELECT * FROM ft_refund WHERE 1=1
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            <if test='refundNo != null and refundNo != ""'>
                AND refund_no = #{refundNo}
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Refund> selectAdminPage(Page<Refund> page,
                                   @Param("status") String status,
                                   @Param("refundNo") String refundNo);
}
