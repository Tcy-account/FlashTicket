package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板数据访问（表 ft_coupon_template）。
 *
 * <p>模板状态机 DRAFT → ACTIVE → PAUSED/TERMINATED，均条件更新。
 * 发布后优惠规则关键字段冻结（version 快照），不在此 Mapper 改动规则字段。
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /** 启用：DRAFT → ACTIVE。 */
    @Update("""
            UPDATE ft_coupon_template
               SET status = 'ACTIVE', version = version + 1
             WHERE id = #{id} AND status = 'DRAFT'
            """)
    int activate(@Param("id") Long id);

    /** 暂停：ACTIVE → PAUSED。 */
    @Update("""
            UPDATE ft_coupon_template
               SET status = 'PAUSED', version = version + 1
             WHERE id = #{id} AND status = 'ACTIVE'
            """)
    int pause(@Param("id") Long id);

    /** 恢复：PAUSED → ACTIVE。 */
    @Update("""
            UPDATE ft_coupon_template
               SET status = 'ACTIVE', version = version + 1
             WHERE id = #{id} AND status = 'PAUSED'
            """)
    int resume(@Param("id") Long id);

    /** 终止：ACTIVE/PAUSED → TERMINATED。 */
    @Update("""
            UPDATE ft_coupon_template
               SET status = 'TERMINATED', version = version + 1
             WHERE id = #{id} AND status IN ('ACTIVE', 'PAUSED')
            """)
    int terminate(@Param("id") Long id);
}
