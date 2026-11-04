package com.flashticket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.inventory.entity.PurchaseLimit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户限购账本数据访问（表 ft_purchase_limit）。
 *
 * <p>限购占用与库存预占同事务；行级串行化依赖「先确保行存在 + FOR UPDATE 锁定」，
 * 再条件更新，防止并发突破每人限购（见 doc/全局数据库模型与ER关系设计.md 七）。
 */
@Mapper
public interface PurchaseLimitMapper extends BaseMapper<PurchaseLimit> {

    /** 锁定限购行（FOR UPDATE），须在事务内、行已存在后调用。 */
    @Select("SELECT * FROM ft_purchase_limit WHERE user_id = #{userId} AND tier_id = #{tierId} FOR UPDATE")
    PurchaseLimit selectForUpdate(@Param("userId") Long userId, @Param("tierId") Long tierId);

    /** 预占占用限购名额：reserved_quantity += q。 */
    @Update("""
            UPDATE ft_purchase_limit
               SET reserved_quantity = reserved_quantity + #{quantity},
                   version           = version + 1
             WHERE user_id = #{userId} AND tier_id = #{tierId}
            """)
    int addReserved(@Param("userId") Long userId, @Param("tierId") Long tierId,
                    @Param("quantity") int quantity);

    /** 确认已售转移名额：reserved -= q，sold += q（仅当 reserved >= q）。 */
    @Update("""
            UPDATE ft_purchase_limit
               SET reserved_quantity = reserved_quantity - #{quantity},
                   sold_quantity     = sold_quantity + #{quantity},
                   version           = version + 1
             WHERE user_id = #{userId} AND tier_id = #{tierId}
               AND reserved_quantity >= #{quantity}
            """)
    int moveReservedToSold(@Param("userId") Long userId, @Param("tierId") Long tierId,
                           @Param("quantity") int quantity);

    /** 释放预占归还名额：reserved -= q（仅当 reserved >= q）。 */
    @Update("""
            UPDATE ft_purchase_limit
               SET reserved_quantity = reserved_quantity - #{quantity},
                   version           = version + 1
             WHERE user_id = #{userId} AND tier_id = #{tierId}
               AND reserved_quantity >= #{quantity}
            """)
    int reduceReserved(@Param("userId") Long userId, @Param("tierId") Long tierId,
                       @Param("quantity") int quantity);
}
