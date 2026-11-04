package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户优惠券数据访问（表 ft_user_coupon）。
 *
 * <p>锁券/核销/返券均条件更新（状态 + 归属 + 有效期），以受影响行数判成败。
 * 锁券原子性是「同一券只能被一个有效订单锁定」的最终保证：只有 AVAILABLE 且
 * 未过期的券能被锁定，并发下仅一个事务成功。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 锁券：AVAILABLE → LOCKED，校验用户归属 + 券未过期。
     *
     * @return 受影响行数（1 成功，0 表示券不可用/非本人/已过期）。
     */
    @Update("""
            UPDATE ft_user_coupon
               SET status = 'LOCKED', locked_at = #{now}, version = version + 1
             WHERE id = #{couponId}
               AND user_id = #{userId}
               AND status = 'AVAILABLE'
               AND valid_until > #{now}
            """)
    int lock(@Param("couponId") Long couponId, @Param("userId") Long userId,
             @Param("now") LocalDateTime now);

    /**
     * 核销：LOCKED → USED（支付成功）。
     *
     * @return 受影响行数（1 成功，0 表示券状态已变，通常为并发）。
     */
    @Update("""
            UPDATE ft_user_coupon
               SET status = 'USED', used_at = #{now}, version = version + 1
             WHERE id = #{couponId} AND status = 'LOCKED'
            """)
    int use(@Param("couponId") Long couponId, @Param("now") LocalDateTime now);

    /**
     * 释放（关单返券）：LOCKED → AVAILABLE（券仍有效）或 EXPIRED（券已过期）。
     *
     * @return 受影响行数（1 成功，0 表示券状态已变）。
     */
    @Update("""
            UPDATE ft_user_coupon
               SET status = CASE WHEN valid_until > #{now} THEN 'AVAILABLE' ELSE 'EXPIRED' END,
                   locked_at = NULL,
                   expired_at = CASE WHEN valid_until <= #{now} THEN #{now} ELSE expired_at END,
                   version = version + 1
             WHERE id = #{couponId} AND status = 'LOCKED'
            """)
    int release(@Param("couponId") Long couponId, @Param("now") LocalDateTime now);
}
