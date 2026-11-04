package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.Campaign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 营销活动数据访问（表 ft_campaign）。
 *
 * <p>状态机转换一律条件更新（以受影响行数判成败），与 event 域活动状态机一致：
 * DRAFT → PENDING_REVIEW → PUBLISHED → ENDED/CANCELLED，驳回回 DRAFT。
 */
@Mapper
public interface CampaignMapper extends BaseMapper<Campaign> {

    /** 提交审核：DRAFT → PENDING_REVIEW。 */
    @Update("""
            UPDATE ft_campaign
               SET status = 'PENDING_REVIEW', version = version + 1
             WHERE id = #{id} AND status = 'DRAFT'
            """)
    int submitReview(@Param("id") Long id);

    /** 审核：PENDING_REVIEW → 目标状态（PUBLISHED 或 DRAFT），记录审核人。 */
    @Update("""
            UPDATE ft_campaign
               SET status = #{target}, reviewed_by = #{reviewerId},
                   reviewed_at = #{now}, version = version + 1
             WHERE id = #{id} AND status = 'PENDING_REVIEW'
            """)
    int review(@Param("id") Long id, @Param("target") String target,
               @Param("reviewerId") Long reviewerId, @Param("now") LocalDateTime now);

    /** 终态转换：PUBLISHED → ENDED/CANCELLED（活动结束/取消）。 */
    @Update("""
            UPDATE ft_campaign
               SET status = #{target}, version = version + 1
             WHERE id = #{id} AND status = 'PUBLISHED'
            """)
    int endOrCancel(@Param("id") Long id, @Param("target") String target);
}
