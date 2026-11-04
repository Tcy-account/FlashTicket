package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.identity.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户身份主体数据访问（表 ft_user）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 后台用户列表分页查询（用户名模糊、状态精确、角色码筛选）。
     */
    @Select("""
            <script>
            SELECT u.* FROM ft_user u WHERE 1=1
            <if test='username != null and username != ""'>
                AND u.username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test='status != null and status != ""'>
                AND u.status = #{status}
            </if>
            <if test='roleCode != null and roleCode != ""'>
                AND EXISTS (
                    SELECT 1 FROM ft_user_role ur
                    JOIN ft_role r ON ur.role_id = r.id
                    WHERE ur.user_id = u.id AND r.code = #{roleCode}
                )
            </if>
            ORDER BY u.created_at DESC
            </script>
            """)
    IPage<User> selectUserPage(Page<User> page,
                               @Param("username") String username,
                               @Param("status") String status,
                               @Param("roleCode") String roleCode);
}
