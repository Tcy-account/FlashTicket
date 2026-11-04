package com.flashticket.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.risk.entity.RiskOperation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控操作审计数据访问（表 ft_risk_operation）。
 */
@Mapper
public interface RiskOperationMapper extends BaseMapper<RiskOperation> {
}
