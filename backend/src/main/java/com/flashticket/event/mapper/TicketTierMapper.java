package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.event.entity.TicketTier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 票档数据访问（表 ft_ticket_tier）。
 */
@Mapper
public interface TicketTierMapper extends BaseMapper<TicketTier> {

    /** 某场次的全部票档（按排序权重升序）。 */
    @Select("SELECT * FROM ft_ticket_tier WHERE session_id = #{sessionId} ORDER BY sort_order ASC, id ASC")
    List<TicketTier> findBySessionId(@Param("sessionId") Long sessionId);

    /** 某场次票档初始总库存（用于场次容量约束校验）。 */
    @Select("SELECT COALESCE(SUM(initial_quantity), 0) FROM ft_ticket_tier WHERE session_id = #{sessionId}")
    long sumInitialQuantityBySessionId(@Param("sessionId") Long sessionId);
}
