package com.flashticket.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.risk.entity.RiskRestriction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控临时限制数据访问（表 ft_risk_restriction）。
 */
@Mapper
public interface RiskRestrictionMapper extends BaseMapper<RiskRestriction> {

    /** 查询主体当前生效的限制（ACTIVE 且未到期），用于准入检查。 */
    @Select("SELECT * FROM ft_risk_restriction " +
            "WHERE subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND status = 'ACTIVE' AND expire_at > #{now}")
    List<RiskRestriction> selectActiveBySubject(@Param("subjectType") String subjectType,
                                                @Param("subjectId") String subjectId,
                                                @Param("now") LocalDateTime now);

    /** 提前解除：ACTIVE → REVOKED，返回受影响行数。 */
    @Update("UPDATE ft_risk_restriction SET status = 'REVOKED', revoked_by = #{revokedBy}, " +
            "revoked_at = #{revokedAt} WHERE id = #{id} AND status = 'ACTIVE'")
    int revoke(@Param("id") Long id, @Param("revokedBy") Long revokedBy,
               @Param("revokedAt") LocalDateTime revokedAt);

    /** 到期自动解除：ACTIVE 且 expire_at <= now → EXPIRED，返回受影响行数。 */
    @Update("UPDATE ft_risk_restriction SET status = 'EXPIRED' " +
            "WHERE status = 'ACTIVE' AND expire_at <= #{now}")
    int expireBatch(@Param("now") LocalDateTime now);
}
