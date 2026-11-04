package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.event.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 演出活动数据访问（表 ft_event）。
 */
@Mapper
public interface EventMapper extends BaseMapper<Event> {

    /**
     * 后台活动分页查询（状态精确、分类精确、标题模糊筛选）。
     */
    @Select("""
            <script>
            SELECT * FROM ft_event WHERE 1=1
            <if test='status != null and status != ""'>
                AND status = #{status}
            </if>
            <if test='categoryId != null'>
                AND category_id = #{categoryId}
            </if>
            <if test='keyword != null and keyword != ""'>
                AND title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<Event> selectEventPage(Page<Event> page,
                                  @Param("status") String status,
                                  @Param("categoryId") Long categoryId,
                                  @Param("keyword") String keyword);

    /**
     * 公开活动分页查询（仅已发布，分类/标题筛选）。
     */
    @Select("""
            <script>
            SELECT * FROM ft_event WHERE status = 'PUBLISHED'
            <if test='categoryId != null'>
                AND category_id = #{categoryId}
            </if>
            <if test='keyword != null and keyword != ""'>
                AND title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY published_at DESC, id DESC
            </script>
            """)
    IPage<Event> selectPublishedPage(Page<Event> page,
                                      @Param("categoryId") Long categoryId,
                                      @Param("keyword") String keyword);
}
