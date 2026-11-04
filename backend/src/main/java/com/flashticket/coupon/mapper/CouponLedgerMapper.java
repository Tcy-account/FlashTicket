package com.flashticket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.coupon.entity.CouponLedger;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券审计流水数据访问（表 ft_coupon_ledger），operation_id 唯一，流水不可变。
 */
@Mapper
public interface CouponLedgerMapper extends BaseMapper<CouponLedger> {
}
