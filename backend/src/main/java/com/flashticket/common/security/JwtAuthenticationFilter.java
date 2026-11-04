package com.flashticket.common.security;

import com.flashticket.identity.service.SessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Bearer Access Token，构建认证主体并挂到 SecurityContext。
 *
 * <p>设计要点（见 doc/用户与账号模块设计.md 四）：
 * <ul>
 *   <li>JWT 本地校验签名与有效期；</li>
 *   <li>会话级撤销：校验 token 携带的 sessionId 在 Redis 中仍活跃，
 *       退出/禁用后立即失效（一次 EXISTS 查询）；</li>
 *   <li>token 缺失 / 非法 / 会话已失效时 <b>不抛异常</b>，仅保持匿名，
 *       由 AuthorizationFilter 拒绝受保护资源并由入口点统一返回 401——
 *       避免因过滤器抛出 AuthenticationException 脱离 ExceptionTranslationFilter
 *       捕获范围导致异常冒泡。</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtService jwtService, SessionService sessionService) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String sessionId = claims.get("sid", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("perms", List.class);

            // 会话撤销即时生效：退出/禁用后 token 即使未过期也拒绝（不设身份 → 401）
            if (sessionId == null || !sessionService.isActive(sessionId)) {
                filterChain.doFilter(request, response);
                return;
            }

            AuthUser principal = new AuthUser(userId, username, sessionId, roles, permissions);

            // 授权粒度：角色（ROLE_xxx） + 权限点（admin:user:list 等），供方法级 @PreAuthorize 使用
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }
            if (permissions != null) {
                permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            // 签名/有效期非法 → 保持匿名，交由授权层统一 401
        }

        filterChain.doFilter(request, response);
    }
}
