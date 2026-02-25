package com.pollen.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.config.RateLimitProperties;
import com.pollen.management.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 增强型 API 速率限制过滤器。
 * <ul>
 *   <li>支持按端点+HTTP方法配置不同限流参数</li>
 *   <li>优先使用 Redis 实现分布式限流，Redis 不可用时回退到内存限流</li>
 *   <li>通过 application.yml 中的 rate-limit 配置项进行灵活配置</li>
 * </ul>
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 内存限流存储（Redis 不可用时的回退方案）
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> requestCounts = new ConcurrentHashMap<>();

    private volatile boolean redisAvailable;

    public RateLimitFilter(RateLimitProperties properties,
                           ObjectMapper objectMapper,
                           StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.redisAvailable = checkRedisAvailable();
    }

    /**
     * 用于测试的构造函数（无 Redis）
     */
    RateLimitFilter() {
        this.properties = new RateLimitProperties();
        this.objectMapper = new ObjectMapper();
        this.redisTemplate = null;
        this.redisAvailable = false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        MatchedEndpoint matched = findMatchingEndpoint(method, path);
        if (matched == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = buildKey(clientIp, matched.normalizedPattern);

        if (isLimitExceeded(key, matched.limit.getMaxRequests(), matched.limit.getWindowMs())) {
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
     * 查找匹配的端点配置。
     * 端点 key 格式：
     *   - "/api/auth/**"          → 匹配所有 HTTP 方法
     *   - "POST:/api/salary/**"   → 仅匹配指定 HTTP 方法
     */
    MatchedEndpoint findMatchingEndpoint(String method, String path) {
        for (Map.Entry<String, RateLimitProperties.EndpointLimit> entry : properties.getEndpoints().entrySet()) {
            String key = entry.getKey();
            String configMethod = null;
            String configPath;

            if (key.contains(":")) {
                String[] parts = key.split(":", 2);
                configMethod = parts[0].toUpperCase();
                configPath = parts[1];
            } else {
                configPath = key;
            }

            if (pathMatcher.match(configPath, path)) {
                if (configMethod == null || configMethod.equalsIgnoreCase(method)) {
                    return new MatchedEndpoint(configPath, entry.getValue());
                }
            }
        }
        return null;
    }

    /**
     * 判断请求路径是否需要速率限制
     */
    boolean isRateLimited(String path) {
        return findMatchingEndpoint(null, path) != null;
    }

    /**
     * 带方法的速率限制判断
     */
    boolean isRateLimited(String method, String path) {
        return findMatchingEndpoint(method, path) != null;
    }

    /**
     * 检查并记录请求，返回是否超出限制。
     * 优先使用 Redis，不可用时回退到内存。
     */
    boolean isLimitExceeded(String key, int maxRequests, long windowMs) {
        if (redisAvailable) {
            try {
                return isLimitExceededRedis(key, maxRequests, windowMs);
            } catch (Exception e) {
                log.warn("Redis 速率限制失败，回退到内存限流: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        return isLimitExceededInMemory(key, maxRequests, windowMs);
    }

    /**
     * Redis 分布式限流实现（滑动窗口）
     */
    private boolean isLimitExceededRedis(String key, int maxRequests, long windowMs) {
        String redisKey = "rate_limit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // 移除过期记录
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

        // 获取当前窗口内的请求数
        Long count = redisTemplate.opsForZSet().zCard(redisKey);
        if (count != null && count >= maxRequests) {
            return true;
        }

        // 添加当前请求
        redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
        redisTemplate.expire(redisKey, windowMs * 2, TimeUnit.MILLISECONDS);
        return false;
    }

    /**
     * 内存限流实现（滑动窗口）
     */
    private boolean isLimitExceededInMemory(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        CopyOnWriteArrayList<Long> timestamps = requestCounts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

        long windowStart = now - windowMs;
        timestamps.removeIf(ts -> ts < windowStart);

        if (timestamps.size() >= maxRequests) {
            return true;
        }

        timestamps.add(now);
        return false;
    }

    /**
     * 构建限流 key：IP + 归一化路径
     */
    private String buildKey(String clientIp, String normalizedPattern) {
        return clientIp + ":" + normalizedPattern;
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
     * 检查 Redis 是否可用
     */
    private boolean checkRedisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis 可用，速率限制使用 Redis 分布式模式");
            return true;
        } catch (Exception e) {
            log.info("Redis 不可用，速率限制使用内存模式: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 清除所有内存速率限制记录（用于测试）
     */
    void clearAll() {
        requestCounts.clear();
    }

    /**
     * 设置 Redis 可用状态（用于测试）
     */
    void setRedisAvailable(boolean available) {
        this.redisAvailable = available;
    }

    boolean isRedisAvailable() {
        return redisAvailable;
    }

    // 暴露常量供测试使用（使用默认配置值）
    static int getMaxRequests() {
        return 20;
    }

    static long getWindowMs() {
        return 60_000L;
    }

    /**
     * 匹配到的端点信息
     */
    static class MatchedEndpoint {
        final String normalizedPattern;
        final RateLimitProperties.EndpointLimit limit;

        MatchedEndpoint(String normalizedPattern, RateLimitProperties.EndpointLimit limit) {
            this.normalizedPattern = normalizedPattern;
            this.limit = limit;
        }
    }
}
