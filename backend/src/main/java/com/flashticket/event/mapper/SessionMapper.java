package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.event.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演出场次数据访问（表 ft_session）。
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    /** 某活动的全部场次（按开始时间升序）。 */
    @Select("SELECT * FROM ft_session WHERE event_id = #{eventId} ORDER BY start_at ASC, id ASC")
    List<Session> findByEventId(@Param("eventId") Long eventId);

    /**
     * 锁定场馆行（FOR UPDATE），作为该场馆排期的串行化入口。
     *
     * <p>创建/修改/延期/取消场次等可能改变时间占用的操作，均须先持有对应
     * ft_venue 行的排他锁，再执行范围冲突检查，避免两个并发操作各自看到
     * 「无冲突」却合起来冲突（见 doc/全局逻辑ER模型.md 五·核心并发问题）。
     */
    @Select("SELECT id FROM ft_venue WHERE id = #{venueId} FOR UPDATE")
    Long lockVenue(@Param("venueId") Long venueId);

    /**
     * 统计与目标时间段冲突的有效场次数（含清场缓冲），须在已持有场馆锁的事务内调用。
     *
     * <p>有效场次仅计 SCHEDULED / POSTPONED（CANCELLED / COMPLETED 不再占用时间）。
     * 冲突判定（buffer 为清场缓冲分钟）：新场次区间 [start, end] 与已有场次
     * [start_at, end_at] 冲突当且仅当 {@code start < end_at + buffer} 且
     * {@code start_at < end + buffer}。
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM ft_session
            WHERE venue_id = #{venueId}
              AND status IN ('SCHEDULED', 'POSTPONED')
            <if test='excludeId != null'>
              AND id != #{excludeId}
            </if>
              AND #{startAt} &lt; DATE_ADD(end_at, INTERVAL #{bufferMinute} MINUTE)
              AND start_at &lt; DATE_ADD(#{endAt}, INTERVAL #{bufferMinute} MINUTE)
            </script>
            """)
    long countTimeConflict(@Param("venueId") Long venueId,
                           @Param("excludeId") Long excludeId,
                           @Param("startAt") LocalDateTime startAt,
                           @Param("endAt") LocalDateTime endAt,
                           @Param("bufferMinute") int bufferMinute);
}
