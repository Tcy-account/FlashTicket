package com.flashticket.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.reconciliation.entity.ReconcileReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账结果汇总数据访问（表 ft_reconcile_report）。
 */
@Mapper
public interface ReconcileReportMapper extends BaseMapper<ReconcileReport> {
}
