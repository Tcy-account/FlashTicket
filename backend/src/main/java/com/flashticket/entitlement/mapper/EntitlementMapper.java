package com.flashticket.entitlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.entitlement.entity.Entitlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 票务权益数据访问（表 ft_ticket_entitlement）。
 */
@Mapper
public interface EntitlementMapper extends BaseMapper<Entitlement> {

    /** 某用户的全部权益（我的门票，按出票时间倒序）。 */
    @Select("SELECT * FROM ft_ticket_entitlement WHERE user_id = #{userId} ORDER BY id DESC")
    List<Entitlement> findByUserId(@Param("userId") Long userId);

    /** 某订单的全部权益（幂等检查：已出票则跳过重复生成）。 */
    @Select("SELECT * FROM ft_ticket_entitlement WHERE order_id = #{orderId} ORDER BY id ASC")
    List<Entitlement> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 撤销某订单全部有效权益（退款，M11）：VALID → REVOKED。
     *
     * <p>条件更新仅命中 VALID 状态，重复撤销幂等（已 REVOKED 的不再更新）。
     */
    @Update("""
            UPDATE ft_ticket_entitlement
               SET status = 'REVOKED', revoked_at = #{now}, version = version + 1,
                   updated_at = #{now}
             WHERE order_id = #{orderId} AND status = 'VALID'
            """)
    int revokeByOrderId(@Param("orderId") Long orderId, @Param("now") LocalDateTime now);
}
