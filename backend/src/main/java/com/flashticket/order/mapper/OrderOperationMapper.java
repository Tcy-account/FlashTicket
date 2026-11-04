package com.flashticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.order.entity.OrderOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单操作流水数据访问（表 ft_order_operation）。
 */
@Mapper
public interface OrderOperationMapper extends BaseMapper<OrderOperation> {

    /** 某订单的全部操作流水（按发生时间正序）。 */
    @Select("SELECT * FROM ft_order_operation WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderOperation> findByOrderId(@Param("orderId") Long orderId);
}
