package com.flashticket.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.identity.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料数据访问（表 ft_user_profile）。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
