package com.flashticket.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.reconciliation.entity.CompensationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 补偿任务数据访问（表 ft_compensation_task）。
 */
@Mapper
public interface CompensationTaskMapper extends BaseMapper<CompensationTask> {

    /** 按幂等操作号查询（同 operationId 不重复创建）。 */
    @Select("SELECT * FROM ft_compensation_task WHERE operation_id = #{operationId} LIMIT 1")
    CompensationTask findByOperationId(@Param("operationId") String operationId);
}
