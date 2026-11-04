package com.flashticket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.inventory.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预占记录数据访问（表 ft_reservation）。
 *
 * <p>request_id 唯一为抢票幂等键；状态转换用条件更新（仅 RESERVED 可转出），
 * 保证「确认与释放竞争时只有一个合法终态」。
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /** 按幂等键查询预占（重复请求直接返回原状态）。 */
    @Select("SELECT * FROM ft_reservation WHERE request_id = #{requestId} LIMIT 1")
    Reservation findByRequestId(@Param("requestId") String requestId);

    /** 条件转换：RESERVED → CONFIRMED，返回受影响行数。 */
    @Update("""
            UPDATE ft_reservation
               SET status = 'CONFIRMED', version = version + 1
             WHERE id = #{id} AND status = 'RESERVED'
            """)
    int confirm(@Param("id") Long id);

    /** 条件转换：RESERVED → RELEASED，返回受影响行数。 */
    @Update("""
            UPDATE ft_reservation
               SET status = 'RELEASED', version = version + 1
             WHERE id = #{id} AND status = 'RESERVED'
            """)
    int release(@Param("id") Long id);

    /** 某票档的全部预占记录（后台查询，按 id 倒序）。 */
    @Select("SELECT * FROM ft_reservation WHERE tier_id = #{tierId} ORDER BY id DESC")
    List<Reservation> findByTierId(@Param("tierId") Long tierId);

    /**
     * 已过期的待支付预占（超时关单扫描，M09 接入），按 id 升序稳定扫描。
     */
    @Select("SELECT * FROM ft_reservation WHERE status = 'RESERVED' AND expire_at < #{now} ORDER BY id ASC")
    List<Reservation> findExpired(@Param("now") LocalDateTime now);
}
