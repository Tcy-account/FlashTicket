package com.flashticket.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MyBatis-Plus 全局配置。
 *
 * <p>1. 时间戳自动填充：created_at / updated_at 统一以 UTC 写入（契约 C09）。
 * 2. 分页插件：后台用户列表等分页查询（M03 起启用）。
 * 3. 乐观锁插件：实体 {@code @Version} 字段在更新时自动带版本条件并自增（M05 起，
 *    用于活动/场次的并发版本控制；未声明 {@code @Version} 的实体不受影响）。
 */
@Configuration
public class MybatisPlusConfig {

    /** 创建 / 更新时间统一 UTC，避免依赖数据库服务器时区（契约：DATETIME(3) UTC）。 */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class,
                        LocalDateTime.now(ZoneOffset.UTC));
            }
        };
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁插件须在分页插件之前（官方建议顺序）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
