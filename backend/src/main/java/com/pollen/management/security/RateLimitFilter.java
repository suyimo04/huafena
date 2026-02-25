package com.pollen.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于内存的 IP 速率限制过滤器。
 * 对包含 /api/auth/ 或 /api/public/ 的路径实施速率限制：每个 IP 每分钟最多 20 次请求。
 * 超出限制返回 429 Too Many Requests。
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!isRateLimited(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + normalizePath(path);

        if (isLimitExceeded(key)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<?> apiResponse = ApiResponse.error(429, "请求过于频繁，请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断请求路径是否需要速率限制（包含 /api/auth/ 或 /api/public/）
     */
    boolean isRateLimited(String path) {
        return path.contains("/api/auth/") || path.contains("/api/public/");
    }

    /**
     * 检查并记录请求，返回是否超出限制
     */
    boolean isLimitExceeded(String key) {
        long now = System.currentTimeMillis();
        CopyOnWriteArrayList<Long> timestamps = requestCounts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

        // 清除过期记录
        long windowStart = now - WINDOW_MS;
        timestamps.removeIf(ts -> ts < windowStart);

        if (timestamps.size() >= MAX_REQUESTS) {
            return true;
        }

        timestamps.add(now);
        return false;
    }

    /**
     * 将路径统一归类，避免不同子路径分散计数
     */
    private String normalizePath(String path) {
        if (path.contains("/api/public/")) {
            return "/api/public/**";
        }
        if (path.contains("/api/auth/")) {
            return "/api/auth/**";
        }
        return path;
    }

    /**
     * 获取客户端真实 IP（支持代理头）
     */
    String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * 清除所有速率限制记录（用于测试）
     */
    void clearAll() {
        requestCounts.clear();
    }

    // 暴露常量供测试使用
    static int getMaxRequests() {
        return MAX_REQUESTS;
    }

    static long getWindowMs() {
        return WINDOW_MS;
    }
}
