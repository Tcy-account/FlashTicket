package com.flashticket.common.config;

import com.flashticket.common.security.JwtAuthenticationFilter;
import com.flashticket.common.security.RestAccessDeniedHandler;
import com.flashticket.common.security.RestAuthenticationEntryPoint;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 完整鉴权链（M02 认证 + M03 RBAC）。
 *
 * <p>策略：
 * <ul>
 *   <li>无状态会话（JWT Access + Redis Refresh）；</li>
 *   <li>匿名放行：健康检查、注册、验证码、登录、刷新、找回/重置密码、后台登录；</li>
 *   <li>其余接口一律认证，由 JwtAuthenticationFilter 填充身份；</li>
 *   <li>管理端接口在 Controller 上用 {@code @PreAuthorize} 做方法级权限点授权；</li>
 *   <li>401/403 返回统一响应信封。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PERMIT_ALL = {
            "/api/v1/health/**",
            "/actuator/**",
            "/api/v1/auth/register",
            "/api/v1/auth/verify-code/send",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password/forgot",
            "/api/v1/auth/password/reset",
            "/api/v1/admin/auth/login",
            // 演出域公开查询（M04：分类/场馆；M05：已发布活动/场次；M06：票档/可售状态）
            "/api/v1/categories",
            "/api/v1/venues",
            "/api/v1/events",
            "/api/v1/events/**",
            "/api/v1/sessions/**",
            "/api/v1/tiers/**",
            // 优惠券公开查询（M13：可领券列表）
            "/api/v1/coupons/available",
            // 通知 SSE 流（M14：EventSource 无自定义请求头，经短期凭证鉴权）
            "/api/v1/notifications/stream",
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 避免 JwtAuthenticationFilter 被 Servlet 容器再次自动注册（已在 Security 链中手动挂载），
     * 防止同一请求被重复解析。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** 第一版统一 BCrypt（见 doc/用户与账号模块设计.md 6.1）。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
