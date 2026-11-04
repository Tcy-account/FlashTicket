package com.flashticket.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 支付业务单数据访问（表 ft_payment）。
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /** 按订单查询支付单（一订单一支付单，不存在返回 null）。 */
    @Select("SELECT * FROM ft_payment WHERE order_id = #{orderId} LIMIT 1")
    Payment findByOrderId(@Param("orderId") Long orderId);

    /**
     * 条件确认支付成功：仅 CREATED/PROCESSING → SUCCEEDED。
     *
     * <p>返回受影响行数（1 表示本次确认生效，0 表示已被并发确认/关闭）。
     * 最终成功支付的唯一性由「订单状态条件更新」在订单侧保证（见支付/关单互斥）。
     */
    @Update("""
            UPDATE ft_payment
               SET status = 'SUCCEEDED', succeeded_at = #{succeededAt},
                   successful_attempt_id = #{attemptId}, version = version + 1,
                   updated_at = #{succeededAt}
             WHERE id = #{id} AND status IN ('CREATED', 'PROCESSING')
            """)
    int markSucceeded(@Param("id") Long id,
                      @Param("attemptId") Long attemptId,
                      @Param("succeededAt") LocalDateTime succeededAt);

    /**
     * 条件关闭支付单（订单关单时同步，仅非终态可关）。
     */
    @Update("""
            UPDATE ft_payment
               SET status = 'CLOSED', closed_at = #{closedAt}, version = version + 1,
                   updated_at = #{closedAt}
             WHERE id = #{id} AND status IN ('CREATED', 'PROCESSING')
            """)
    int markClosed(@Param("id") Long id, @Param("closedAt") LocalDateTime closedAt);

    /** 管理端支付分页（按 id 倒序，可按状态/订单号筛选）。 */
    @Select("""
            <script>
            SELECT * FROM ft_payment WHERE 1=1
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            <if test='paymentNo != null and paymentNo != ""'>
                AND payment_no = #{paymentNo}
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Payment> selectAdminPage(Page<Payment> page,
                                    @Param("status") String status,
                                    @Param("paymentNo") String paymentNo);
}
