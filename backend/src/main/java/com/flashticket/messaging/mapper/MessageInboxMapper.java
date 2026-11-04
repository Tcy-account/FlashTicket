package com.flashticket.messaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.messaging.entity.MessageInbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 消息消费幂等记录数据访问（表 ft_message_inbox）。
 */
@Mapper
public interface MessageInboxMapper extends BaseMapper<MessageInbox> {

    /** 判断事件是否已被该消费者处理过（>0 即已消费，直接幂等跳过）。 */
    @Select("SELECT COUNT(*) FROM ft_message_inbox WHERE consumer_name = #{consumerName} AND event_id = #{eventId}")
    int countByConsumerAndEvent(@Param("consumerName") String consumerName,
                                @Param("eventId") String eventId);
}
