package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.event.entity.EventCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 演出分类数据访问（表 ft_event_category）。
 */
@Mapper
public interface EventCategoryMapper extends BaseMapper<EventCategory> {

    /** 按分类码查询（唯一性校验），不存在返回 null。 */
    @Select("SELECT * FROM ft_event_category WHERE code = #{code} LIMIT 1")
    EventCategory findByCode(@Param("code") String code);

    /** 是否存在同码分类（排除自身），用于更新时唯一性校验。 */
    @Select("SELECT COUNT(*) FROM ft_event_category WHERE code = #{code} AND id != #{excludeId}")
    long countByCodeExcluding(@Param("code") String code, @Param("excludeId") Long excludeId);
}
