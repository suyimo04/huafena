package com.pollen.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 速率限制配置属性。
 * 支持按端点配置不同的限流参数。
 * 端点 key 格式：
 *   - "/api/auth/**"          → 匹配所有方法
 *   - "POST:/api/salary/**"   → 仅匹配 POST 方法
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /** 默认每窗口最大请求数 */
    private int defaultMaxRequests = 20;

    /** 默认时间窗口（毫秒） */
    private long defaultWindowMs = 60_000L;

    /** 端点级别的限流配置 */
    private Map<String, EndpointLimit> endpoints = new LinkedHashMap<>();

    @Data
    public static class EndpointLimit {
        private int maxRequests = 20;
        private long windowMs = 60_000L;
    }
}
