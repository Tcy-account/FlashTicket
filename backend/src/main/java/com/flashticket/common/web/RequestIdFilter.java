package com.flashticket.common.web;

import com.flashticket.common.api.RequestIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求标识过滤器：为每个请求生成 requestId 注入 MDC 与响应头。
 *
 * <p>响应头 X-Request-Id 供前端 API Lab 展示；traceId 由后续可观测组件
 * （Micrometer Tracing，M12/M17 阶段接入）写入 MDC，此处仅透传。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            MDC.put(RequestIdHolder.MDC_REQUEST_ID, requestId);
            response.setHeader(HEADER_REQUEST_ID, requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdHolder.MDC_REQUEST_ID);
            MDC.remove(RequestIdHolder.MDC_TRACE_ID);
        }
    }
}
