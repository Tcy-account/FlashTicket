package com.flashticket.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.payment.entity.PaymentAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 支付执行尝试数据访问（表 ft_payment_attempt）。
 */
@Mapper
public interface PaymentAttemptMapper extends BaseMapper<PaymentAttempt> {

    /** 按客户端幂等请求号查询（不存在返回 null）。 */
    @Select("SELECT * FROM ft_payment_attempt WHERE attempt_request_id = #{requestId} LIMIT 1")
    PaymentAttempt findByAttemptRequestId(@Param("requestId") String requestId);

    /** 某支付业务已有尝试次数（用于计算下一个 attempt_no）。 */
    @Select("SELECT COUNT(*) FROM ft_payment_attempt WHERE payment_id = #{paymentId}")
    int countByPaymentId(@Param("paymentId") Long paymentId);

    /** 条件确认尝试成功。 */
    @Update("""
            UPDATE ft_payment_attempt
               SET status = 'SUCCEEDED', finished_at = #{finishedAt}, updated_at = #{finishedAt}
             WHERE id = #{id} AND status IN ('CREATED', 'PROCESSING')
            """)
    int markSucceeded(@Param("id") Long id, @Param("finishedAt") LocalDateTime finishedAt);
}
