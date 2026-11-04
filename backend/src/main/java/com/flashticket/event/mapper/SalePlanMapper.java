package com.flashticket.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.event.entity.SalePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 售票计划数据访问（表 ft_sale_plan）。
 */
@Mapper
public interface SalePlanMapper extends BaseMapper<SalePlan> {

    /** 某票档的售票计划（一对一）。 */
    @Select("SELECT * FROM ft_sale_plan WHERE tier_id = #{tierId} LIMIT 1")
    SalePlan findByTierId(@Param("tierId") Long tierId);

    /** 批量按票档 ID 查询售票计划（供审核指纹与场次票档聚合）。 */
    @Select("""
            <script>
            SELECT * FROM ft_sale_plan WHERE tier_id IN
            <foreach collection="tierIds" item="tid" open="(" separator="," close=")">
                #{tid}
            </foreach>
            ORDER BY tier_id ASC
            </script>
            """)
    List<SalePlan> findByTierIds(@Param("tierIds") List<Long> tierIds);
}
