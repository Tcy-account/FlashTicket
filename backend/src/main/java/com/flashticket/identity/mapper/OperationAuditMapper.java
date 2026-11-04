package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.identity.entity.OperationAudit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台敏感操作审计数据访问（表 ft_operation_audit）。
 */
@Mapper
public interface OperationAuditMapper extends BaseMapper<OperationAudit> {
}
