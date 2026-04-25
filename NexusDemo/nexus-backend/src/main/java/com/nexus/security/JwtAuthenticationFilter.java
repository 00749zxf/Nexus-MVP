package com.nexus.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            log.info("请求URI: {}, 解析到的JWT: {}", request.getRequestURI(), jwt != null ? "存在(长度:" + jwt.length() + ")" : "不存在");

            if (jwt != null) {
                try {
                    boolean isValid = jwtUtil.isTokenValid(jwt);
                    log.info("JWT验证结果: {}", isValid);

                    if (isValid) {
                        String username = jwtUtil.getUsernameFromToken(jwt);
                        log.info("JWT有效, 用户名: {}", username);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("设置认证用户成功: {}", username);
                    } else {
                        log.warn("JWT无效或已过期");
                    }
                } catch (Exception e) {
                    log.error("JWT验证异常: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(jwtUtil.getTokenHeader());
        log.debug("Authorization Header: {}", headerAuth);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(jwtUtil.getTokenPrefix())) {
            return jwtUtil.extractToken(headerAuth);
        }

        return null;
    }
}