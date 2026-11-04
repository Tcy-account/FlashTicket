package com.flashticket.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.reconciliation.entity.ReconcileException;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 异常工单数据访问（表 ft_reconcile_exception）。
 */
@Mapper
public interface ReconcileExceptionMapper extends BaseMapper<ReconcileException> {

    /**
     * 幂等去重：同一业务实体（领域+检查项+实体）是否存在仍未关闭的工单。
     * 存在则本次扫描跳过，避免重复生成未关闭工单。
     */
    @Select("""
            SELECT COUNT(*) FROM ft_reconcile_exception
             WHERE scope = #{scope} AND check_type = #{checkType}
               AND entity_type = #{entityType} AND entity_id = #{entityId}
               AND status IN ('OPEN','INVESTIGATING')
            """)
    int countOpenByEntity(@Param("scope") String scope,
                          @Param("checkType") String checkType,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId);
}
