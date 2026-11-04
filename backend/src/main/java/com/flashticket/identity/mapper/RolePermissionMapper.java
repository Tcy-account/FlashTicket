package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.identity.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色与权限关系数据访问（表 ft_role_permission）。
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /** 删除某角色的全部权限关系（配置角色权限时整体替换）。 */
    @Delete("DELETE FROM ft_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
