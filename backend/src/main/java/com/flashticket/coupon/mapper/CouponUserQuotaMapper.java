package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.CouponUserQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户领取额度账本数据访问（表 ft_coupon_user_quota）。
 *
 * <p>并发限领靠「先确保行存在 + FOR UPDATE 锁定」再累加计数，防止并发绕过
 * 每人限领（与 inventory 域 ft_purchase_limit 同款模式）。
 */
@Mapper
public interface CouponUserQuotaMapper extends BaseMapper<CouponUserQuota> {

    /** 锁定限领行（FOR UPDATE），须在事务内、行已存在后调用。 */
    @Select("""
            SELECT * FROM ft_coupon_user_quota
             WHERE user_id = #{userId} AND template_id = #{templateId}
             FOR UPDATE
            """)
    CouponUserQuota selectForUpdate(@Param("userId") Long userId, @Param("templateId") Long templateId);

    /** 累加已领取数量（+1）。 */
    @Update("""
            UPDATE ft_coupon_user_quota
               SET claimed_count = claimed_count + 1,
                   version       = version + 1
             WHERE user_id = #{userId} AND template_id = #{templateId}
            """)
    int incrementClaimed(@Param("userId") Long userId, @Param("templateId") Long templateId);
}
