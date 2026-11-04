package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.identity.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色定义数据访问（表 ft_role）。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /** 查询用户拥有的角色码（走 ft_user_role 关联）。 */
    @Select("SELECT r.code FROM ft_role r JOIN ft_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> findCodesByUserId(@Param("userId") Long userId);

    /** 按角色码查角色 ID，不存在返回 null。 */
    @Select("SELECT id FROM ft_role WHERE code = #{code}")
    Long findIdByCode(@Param("code") String code);
}
