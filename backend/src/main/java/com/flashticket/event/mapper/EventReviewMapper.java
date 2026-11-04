package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.event.entity.EventReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动审核记录数据访问（表 ft_event_review）。
 */
@Mapper
public interface EventReviewMapper extends BaseMapper<EventReview> {

    /** 某活动的审核记录（按时间升序）。 */
    @Select("SELECT * FROM ft_event_review WHERE event_id = #{eventId} ORDER BY id ASC")
    List<EventReview> findByEventId(@Param("eventId") Long eventId);

    /** 某活动最近一条审核记录。 */
    @Select("SELECT * FROM ft_event_review WHERE event_id = #{eventId} ORDER BY id DESC LIMIT 1")
    EventReview findLatestByEventId(@Param("eventId") Long eventId);
}
