package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.CouponUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 优惠券锁券核销记录数据访问（表 ft_coupon_usage）。
 *
 * <p>同一券同一订单唯一（uk_usage_coupon_order），记录一次锁券生命周期的
 * LOCKED → USED / RELEASED 状态转换，供审计与对账。
 */
@Mapper
public interface CouponUsageMapper extends BaseMapper<CouponUsage> {

    /** 标记核销：LOCKED → USED。 */
    @Update("""
            UPDATE ft_coupon_usage
               SET status = 'USED', used_at = #{now}
             WHERE user_coupon_id = #{userCouponId} AND order_id = #{orderId} AND status = 'LOCKED'
            """)
    int markUsed(@Param("userCouponId") Long userCouponId, @Param("orderId") Long orderId,
                 @Param("now") LocalDateTime now);

    /** 标记释放：LOCKED → RELEASED。 */
    @Update("""
            UPDATE ft_coupon_usage
               SET status = 'RELEASED', released_at = #{now}
             WHERE user_coupon_id = #{userCouponId} AND order_id = #{orderId} AND status = 'LOCKED'
            """)
    int markReleased(@Param("userCouponId") Long userCouponId, @Param("orderId") Long orderId,
                     @Param("now") LocalDateTime now);
}
