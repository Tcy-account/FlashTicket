package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.identity.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户与角色关系数据访问（表 ft_user_role）。
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /** 删除某用户的全部角色关系（角色分配整体替换）。 */
    @Delete("DELETE FROM ft_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 统计持有某角色且账号仍 ACTIVE 的用户数（保护最后一个超级管理员）。 */
    @Select("""
            SELECT COUNT(*)
            FROM ft_user_role ur
            JOIN ft_user u ON ur.user_id = u.id
            WHERE ur.role_id = #{roleId} AND u.status = 'ACTIVE'
            """)
    long countActiveUsersByRoleId(@Param("roleId") Long roleId);

    /** 用户是否已持有某角色。 */
    @Select("SELECT COUNT(*) FROM ft_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    long countByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 查询持有某角色的全部用户 ID。 */
    @Select("SELECT user_id FROM ft_user_role WHERE role_id = #{roleId}")
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);

    /** 批量查询用户的角色码（返回 {userId, code} 映射）。 */
    @Select("""
            <script>
            SELECT ur.user_id AS userId, r.code AS code
            FROM ft_user_role ur
            JOIN ft_role r ON ur.role_id = r.id
            WHERE ur.user_id IN
            <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            ORDER BY r.id
            </script>
            """)
    List<Map<String, Object>> findRoleCodesByUserIds(@Param("userIds") List<Long> userIds);
}
