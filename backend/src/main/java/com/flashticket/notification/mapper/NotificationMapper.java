package com.flashticket.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知内容数据访问（表 ft_notification）。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
