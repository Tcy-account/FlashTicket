package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.identity.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限点数据访问（表 ft_permission）。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 聚合用户所有角色的有效权限码（去重），用于签发 JWT 与前端渲染。 */
    @Select("""
            SELECT DISTINCT p.code
            FROM ft_permission p
            JOIN ft_role_permission rp ON p.id = rp.permission_id
            JOIN ft_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId} AND p.status = 'ACTIVE'
            """)
    List<String> findCodesByUserId(@Param("userId") Long userId);

    /** 查询某角色绑定的全部权限点。 */
    @Select("""
            SELECT p.*
            FROM ft_permission p
            JOIN ft_role_permission rp ON p.id = rp.permission_id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.id
            """)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
