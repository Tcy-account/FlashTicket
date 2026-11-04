package com.flashticket.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.reconciliation.entity.ReconcileAction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异常处理记录数据访问（表 ft_reconcile_action）。
 */
@Mapper
public interface ReconcileActionMapper extends BaseMapper<ReconcileAction> {
}
