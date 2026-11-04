package com.flashticket.refund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.refund.entity.RefundAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 退款执行尝试数据访问（表 ft_refund_attempt）。
 */
@Mapper
public interface RefundAttemptMapper extends BaseMapper<RefundAttempt> {

    /** 某退款业务的尝试次数（用于计算下一次 attempt_no）。 */
    @Select("SELECT COUNT(*) FROM ft_refund_attempt WHERE refund_id = #{refundId}")
    int countByRefundId(@Param("refundId") Long refundId);

    /** 按退款业务与尝试序号查询（幂等重查，不存在返回 null）。 */
    @Select("SELECT * FROM ft_refund_attempt WHERE refund_id = #{refundId} AND attempt_no = #{attemptNo} LIMIT 1")
    RefundAttempt findByRefundIdAndAttemptNo(@Param("refundId") Long refundId,
                                              @Param("attemptNo") Integer attemptNo);

    /** 条件确认尝试成功：仅非终态 → SUCCEEDED。 */
    @Update("""
            UPDATE ft_refund_attempt
               SET status = 'SUCCEEDED', finished_at = #{finishedAt}, updated_at = #{finishedAt}
             WHERE id = #{id} AND status IN ('CREATED', 'PROCESSING')
            """)
    int markSucceeded(@Param("id") Long id, @Param("finishedAt") LocalDateTime finishedAt);
}
