package com.flashticket.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.payment.entity.PaymentCallback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 模拟支付回调数据访问（表 ft_payment_callback）。
 */
@Mapper
public interface PaymentCallbackMapper extends BaseMapper<PaymentCallback> {

    /** 按 (channel, channel_event_id) 查询（回调去重，不存在返回 null）。 */
    @Select("""
            SELECT * FROM ft_payment_callback
             WHERE channel = #{channel} AND channel_event_id = #{eventId} LIMIT 1
            """)
    PaymentCallback findByChannelEventId(@Param("channel") String channel,
                                         @Param("eventId") String eventId);
}
