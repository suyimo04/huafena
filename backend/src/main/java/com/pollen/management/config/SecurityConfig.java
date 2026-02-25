package com.pollen.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApiResponse;
import com.pollen.management.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置：BCrypt、JWT 过滤器、CORS、安全头、权限映射
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.pollen.management.security.RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（无状态 JWT）
                .csrf(csrf -> csrf.disable())
                // CORS 配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 安全头
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )
                // 无状态会话管理
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 权限配置
                .authorizeHttpRequests(auth -> auth
                        // 公开接口白名单
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        // 管理员专属接口
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 数据看板：ADMIN、LEADER
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "LEADER")
                        // 薪资管理：写操作 ADMIN、LEADER；读操作 ADMIN、LEADER、VICE_LEADER、MEMBER
                        .requestMatchers(HttpMethod.POST, "/api/salary/**").hasAnyRole("ADMIN", "LEADER")
                        .requestMatchers(HttpMethod.PUT, "/api/salary/**").hasAnyRole("ADMIN", "LEADER")
                        .requestMatchers(HttpMethod.DELETE, "/api/salary/**").hasAnyRole("ADMIN", "LEADER")
                        .requestMatchers(HttpMethod.GET, "/api/salary/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER", "MEMBER")
                        // AI 面试管理：写操作 ADMIN、LEADER；读操作 ADMIN、LEADER、VICE_LEADER
                        .requestMatchers(HttpMethod.POST, "/api/interviews/**").hasAnyRole("ADMIN", "LEADER")
                        .requestMatchers(HttpMethod.PUT, "/api/interviews/**").hasAnyRole("ADMIN", "LEADER")
                        .requestMatchers(HttpMethod.DELETE, "/api/interviews/**").hasAnyRole("ADMIN", "LEADER")
                        .requestMatchers(HttpMethod.GET, "/api/interviews/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER")
                        // 申请管理：ADMIN、LEADER、VICE_LEADER
                        .requestMatchers("/api/applications/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER")
                        // 活动管理：ADMIN、LEADER、VICE_LEADER、MEMBER、INTERN
                        .requestMatchers("/api/activities/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER", "MEMBER", "INTERN")
                        // 积分管理：ADMIN、LEADER、VICE_LEADER、MEMBER、INTERN
                        .requestMatchers("/api/points/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER", "MEMBER", "INTERN")
                        // 问卷模板管理：ADMIN、LEADER、VICE_LEADER
                        .requestMatchers("/api/questionnaire/templates/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER")
                        // 问卷回答：已认证用户
                        .requestMatchers("/api/questionnaire/responses/**").authenticated()
                        // 报表与数据导出：ADMIN、LEADER
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "LEADER")
                        // 邮件服务：ADMIN、LEADER
                        .requestMatchers("/api/emails/**").hasAnyRole("ADMIN", "LEADER")
                        // 成员管理：ADMIN、LEADER、VICE_LEADER（列表/详情），已认证用户（状态/心跳）
                        .requestMatchers(HttpMethod.GET, "/api/members/**").hasAnyRole("ADMIN", "LEADER", "VICE_LEADER")
                        .requestMatchers(HttpMethod.PUT, "/api/members/*/status").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/members/*/heartbeat").authenticated()
                        // 成员流转管理：ADMIN、LEADER
                        .requestMatchers("/api/member-rotation/**").hasAnyRole("ADMIN", "LEADER")
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )
                // 自定义异常处理：401 和 403 返回 JSON
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 添加速率限制过滤器（在 JWT 之前）
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 自定义 AuthenticationEntryPoint：Token 无效/过期返回 401 JSON 响应
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> body = ApiResponse.error(401, "登录已过期，请重新登录");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    /**
     * 自定义 AccessDeniedHandler：权限不足返回 403 JSON 响应
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> body = ApiResponse.error(403, "无权访问该资源");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
