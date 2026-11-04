package com.flashticket.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.risk.entity.RiskRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控规则数据访问（表 ft_risk_rule）。
 */
@Mapper
public interface RiskRuleMapper extends BaseMapper<RiskRule> {
}
