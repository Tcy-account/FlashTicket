package com.flashticket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存总账数据访问（表 ft_inventory）。
 *
 * <p>三态账本变更一律走条件更新（available/reserved 下界校验 + version 自增），
 * 以受影响行数判断是否成功；这是防超卖、防重复释放的最终安全边界，不依赖
 * 先读后写（见 doc/跨域一致性与工程落地审查.md C04）。
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 预占：available -= q，reserved += q；仅当 available >= q 才生效。
     *
     * @return 受影响行数（1 成功，0 表示可售不足）。
     */
    @Update("""
            UPDATE ft_inventory
               SET available = available - #{quantity},
                   reserved  = reserved + #{quantity},
                   version   = version + 1
             WHERE tier_id = #{tierId}
               AND available >= #{quantity}
            """)
    int reserve(@Param("tierId") Long tierId, @Param("quantity") int quantity);

    /**
     * 确认已售：reserved -= q，sold += q；仅当 reserved >= q 才生效。
     *
     * @return 受影响行数（1 成功，0 表示预占不足，通常为状态已变）。
     */
    @Update("""
            UPDATE ft_inventory
               SET reserved = reserved - #{quantity},
                   sold     = sold + #{quantity},
                   version  = version + 1
             WHERE tier_id = #{tierId}
               AND reserved >= #{quantity}
            """)
    int confirm(@Param("tierId") Long tierId, @Param("quantity") int quantity);

    /**
     * 释放预占：reserved -= q，available += q；仅当 reserved >= q 才生效。
     *
     * @return 受影响行数（1 成功，0 表示预占不足，通常为状态已变）。
     */
    @Update("""
            UPDATE ft_inventory
               SET reserved  = reserved - #{quantity},
                   available = available + #{quantity},
                   version   = version + 1
             WHERE tier_id = #{tierId}
               AND reserved >= #{quantity}
            """)
    int release(@Param("tierId") Long tierId, @Param("quantity") int quantity);
}
