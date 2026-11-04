package com.flashticket.risk.api;

import com.flashticket.common.web.ClientIpUtils;
import com.flashticket.risk.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 公开查询 IP 限流拦截器（M15-A）。
 *
 * <p>对活动/分类/场馆/场次/票档等公开查询按客户端 IP 限流（同 IP 每分钟 N 次），
 * 防止恶意爬取与流量冲击。超限抛 {@code RISK_RATE_LIMITED}（429），由全局异常
 * 处理器统一转响应信封。Redis 不可用时 fail-open 放行。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        rateLimitService.checkQueryIp(ClientIpUtils.resolve(request));
        return true;
    }
}
