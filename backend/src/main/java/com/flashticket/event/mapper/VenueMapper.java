package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.event.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 演出场馆数据访问（表 ft_venue）。
 */
@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

    /**
     * 场馆分页查询（城市精确、状态精确、名称模糊筛选）。
     */
    @Select("""
            <script>
            SELECT * FROM ft_venue WHERE 1=1
            <if test='city != null and city != ""'>
                AND city = #{city}
            </if>
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            <if test='name != null and name != ""'>
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Venue> selectVenuePage(Page<Venue> page,
                                 @Param("city") String city,
                                 @Param("status") String status,
                                 @Param("name") String name);
}
