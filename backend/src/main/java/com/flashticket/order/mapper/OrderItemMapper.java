package com.flashticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细交易快照数据访问（表 ft_order_item）。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /** 某订单的全部明细（按 id 升序）。 */
    @Select("SELECT * FROM ft_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    /** 批量按订单 ID 查询明细（供订单列表聚合，避免 N+1）。 */
    @Select("""
            <script>
            SELECT * FROM ft_order_item WHERE order_id IN
            <foreach collection="orderIds" item="oid" open="(" separator="," close=")">
                #{oid}
            </foreach>
            ORDER BY order_id ASC, id ASC
            </script>
            """)
    List<OrderItem> findByOrderIds(@Param("orderIds") List<Long> orderIds);
}
