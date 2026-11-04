package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.CouponQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券发行额度账本数据访问（表 ft_coupon_quota）。
 *
 * <p>防超发最终边界：条件更新 available >= q，以受影响行数判成败，不依赖先读后写。
 * 三态守恒 total = available + reserved + issued 由数据库 CHECK 兜底。
 */
@Mapper
public interface CouponQuotaMapper extends BaseMapper<CouponQuota> {

    /**
     * 同步发券扣减额度：available -= 1，issued += 1；仅当 available >= 1。
     *
     * @return 受影响行数（1 成功，0 表示已领完）。
     */
    @Update("""
            UPDATE ft_coupon_quota
               SET available = available - 1,
                   issued    = issued + 1,
                   version   = version + 1
             WHERE template_id = #{templateId}
               AND available >= 1
            """)
    int deductIssue(@Param("templateId") Long templateId);
}
