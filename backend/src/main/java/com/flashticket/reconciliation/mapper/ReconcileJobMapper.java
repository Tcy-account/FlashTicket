package com.flashticket.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.reconciliation.entity.ReconcileJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账任务数据访问（表 ft_reconcile_job）。
 */
@Mapper
public interface ReconcileJobMapper extends BaseMapper<ReconcileJob> {
}
