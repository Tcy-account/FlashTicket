package com.flashticket.common.config;

import com.flashticket.risk.api.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（M15-A）：注册公开查询 IP 限流拦截器。
 *
 * <p>仅拦截用户端公开查询路径（活动/分类/场馆/场次/票档），管理端路径
 * （/api/v1/admin/**）与个人业务接口不受影响。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/categories",
                        "/api/v1/venues",
                        "/api/v1/events/**",
                        "/api/v1/sessions/**",
                        "/api/v1/tiers/**");
    }
}
