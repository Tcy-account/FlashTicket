package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.CouponClaimRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 领券请求数据访问（表 ft_coupon_claim_request），request_id 唯一幂等。
 *
 * <p>第一版同步领券在事务内落 SUCCEEDED/FAILED；六态字段为后续异步发券预留。
 */
@Mapper
public interface CouponClaimRequestMapper extends BaseMapper<CouponClaimRequest> {

    /** 按请求号查询（幂等判定）。 */
    @Select("SELECT * FROM ft_coupon_claim_request WHERE request_id = #{requestId}")
    CouponClaimRequest findByRequestId(@Param("requestId") String requestId);

    /** 标记发放成功（记录用户券 ID）。 */
    @Update("""
            UPDATE ft_coupon_claim_request
               SET status = 'SUCCEEDED', user_coupon_id = #{userCouponId}, version = version + 1
             WHERE request_id = #{requestId}
            """)
    int markSucceeded(@Param("requestId") String requestId, @Param("userCouponId") Long userCouponId);

    /** 标记失败（记录失败原因码）。 */
    @Update("""
            UPDATE ft_coupon_claim_request
               SET status = 'FAILED', fail_reason = #{failReason}, version = version + 1
             WHERE request_id = #{requestId}
            """)
    int markFailed(@Param("requestId") String requestId, @Param("failReason") String failReason);
}
