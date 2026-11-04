package com.flashticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主体数据访问（表 ft_order）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 按抢票请求号查询订单（幂等/归属判断）。 */
    @Select("SELECT * FROM ft_order WHERE request_id = #{requestId} LIMIT 1")
    Order findByRequestId(@Param("requestId") String requestId);

    /**
     * 条件关单：仅当订单仍为 PENDING_PAYMENT 时转为 CLOSED。
     *
     * <p>返回受影响行数（1 表示抢到状态转换权，0 表示已被并发支付/关闭）。
     * 这是支付与关单争夺订单状态转换权的最终边界（见 doc/订单与交易状态机模块.md 七）。
     */
    @Update("""
            UPDATE ft_order
               SET status = 'CLOSED', closed_at = #{closedAt}, close_reason = #{closeReason},
                   version = version + 1, updated_at = #{closedAt}
             WHERE id = #{id} AND status = 'PENDING_PAYMENT'
            """)
    int closeById(@Param("id") Long id,
                  @Param("closedAt") LocalDateTime closedAt,
                  @Param("closeReason") String closeReason);

    /**
     * 条件支付确认：仅当订单仍为 PENDING_PAYMENT 且未过期时转为 PAID。
     *
     * <p>返回受影响行数（1 表示抢到状态转换权，0 表示已被并发关单/支付或已过期）。
     * 与 {@link #closeById} 对称，二者在 MySQL 行锁 + 当前读语义下天然互斥，
     * 是支付成功与超时关单竞争的唯一合法终态边界（见 doc/模拟支付与退款模块.md 五）。
     * 附带 expire_at &gt; now 校验，保证「已到期但关单任务尚未执行」的订单也不能被支付。
     */
    @Update("""
            UPDATE ft_order
               SET status = 'PAID', paid_at = #{paidAt}, paid_amount_cent = #{paidAmount},
                   version = version + 1, updated_at = #{paidAt}
             WHERE id = #{id} AND status = 'PENDING_PAYMENT' AND expire_at > #{now}
            """)
    int paidById(@Param("id") Long id,
                 @Param("paidAmount") Long paidAmount,
                 @Param("paidAt") LocalDateTime paidAt,
                 @Param("now") LocalDateTime now);

    /** 扫描支付期限已到、仍为待支付的订单（按 id 升序，供定时关单兜底）。 */
    @Select("""
            SELECT * FROM ft_order
             WHERE status = 'PENDING_PAYMENT' AND expire_at <= #{now}
             ORDER BY id ASC
             LIMIT #{limit}
            """)
    List<Order> selectExpiredPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 我的订单分页（按创建时间倒序，可按状态筛选）。 */
    @Select("""
            <script>
            SELECT * FROM ft_order WHERE user_id = #{userId}
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Order> selectUserPage(Page<Order> page,
                                @Param("userId") Long userId,
                                @Param("status") String status);

    /** 管理端订单分页（按创建时间倒序，可按状态/订单号/用户筛选）。 */
    @Select("""
            <script>
            SELECT * FROM ft_order WHERE 1=1
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            <if test='orderNo != null and orderNo != ""'>
                AND order_no = #{orderNo}
            </if>
            <if test='userId != null'>
                AND user_id = #{userId}
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Order> selectAdminPage(Page<Order> page,
                                 @Param("status") String status,
                                 @Param("orderNo") String orderNo,
                                 @Param("userId") Long userId);
}
