package com.pollen.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain filterChain;

    /**
     * 构建包含所有敏感端点配置的 RateLimitProperties，
     * 与 application.yml 中的配置保持一致。
     */
    private static RateLimitProperties buildFullProperties() {
        RateLimitProperties props = new RateLimitProperties();
        props.setDefaultMaxRequests(20);
        props.setDefaultWindowMs(60_000L);

        Map<String, RateLimitProperties.EndpointLimit> endpoints = new LinkedHashMap<>();

        // 认证接口
        endpoints.put("/api/auth/**", limit(20, 60_000L));
        // 公开链接
        endpoints.put("/api/public/**", limit(20, 60_000L));
        // 薪资操作
        endpoints.put("POST:/api/salary/**", limit(10, 60_000L));
        endpoints.put("PUT:/api/salary/**", limit(10, 60_000L));
        // 面试操作
        endpoints.put("POST:/api/interviews/**", limit(10, 60_000L));
        // 批量操作
        endpoints.put("POST:/api/applications/batch-approve", limit(5, 60_000L));
        endpoints.put("POST:/api/applications/batch-reject", limit(5, 60_000L));
        endpoints.put("POST:/api/applications/batch-notify-interview", limit(5, 60_000L));
        // 数据导出
        endpoints.put("GET:/api/applications/export", limit(5, 60_000L));
        endpoints.put("POST:/api/reports/weekly/generate", limit(5, 60_000L));
        endpoints.put("GET:/api/reports/export/**", limit(10, 60_000L));
        // 邮件发送
        endpoints.put("POST:/api/emails/send", limit(10, 60_000L));
        endpoints.put("PUT:/api/emails/config", limit(5, 60_000L));
        // 用户管理
        endpoints.put("POST:/api/users/**", limit(10, 60_000L));
        endpoints.put("PUT:/api/users/**", limit(10, 60_000L));
        endpoints.put("DELETE:/api/users/**", limit(5, 60_000L));
        // 实习期管理
        endpoints.put("POST:/api/internships/*/convert", limit(5, 60_000L));
        endpoints.put("POST:/api/internships/*/terminate", limit(5, 60_000L));
        endpoints.put("POST:/api/internships/*/extend", limit(5, 60_000L));
        // 成员状态
        endpoints.put("PUT:/api/members/*/status", limit(20, 60_000L));
        // 备份操作
        endpoints.put("POST:/api/backups/**", limit(3, 60_000L));

        props.setEndpoints(endpoints);
        return props;
    }

    private static RateLimitProperties.EndpointLimit limit(int maxRequests, long windowMs) {
        RateLimitProperties.EndpointLimit l = new RateLimitProperties.EndpointLimit();
        l.setMaxRequests(maxRequests);
        l.setWindowMs(windowMs);
        return l;
    }

    @BeforeEach
    void setUp() {
        RateLimitProperties props = buildFullProperties();
        filter = new RateLimitFilter(props, new ObjectMapper(), null);
        filter.clearAll();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestsUnderLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws ServletException, IOException {
        // Send 20 requests to hit the limit
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
        }

        // 21st request should be blocked
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("429"));
        assertTrue(response.getContentAsString().contains("请求过于频繁"));
    }

    @Test
    void shouldNotRateLimitNonSensitivePaths() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/questionnaire/templates");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRateLimitAuthPaths() {
        assertTrue(filter.isRateLimited("/api/auth/login"));
        assertTrue(filter.isRateLimited("/api/auth/register"));
    }

    @Test
    void shouldRateLimitPublicPaths() {
        assertTrue(filter.isRateLimited("/api/public/questionnaire/abc123"));
        assertTrue(filter.isRateLimited("/api/public/questionnaire/abc123/submit"));
    }

    @Test
    void shouldNotRateLimitOtherPaths() {
        assertFalse(filter.isRateLimited("/api/questionnaire/templates"));
    }

    @Test
    void shouldTrackDifferentIpsSeparately() throws ServletException, IOException {
        // Exhaust limit for IP 1
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
        }

        // IP 2 should still be allowed
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, atLeast(1)).doFilter(request, response);
        assertNotEquals(429, response.getStatus());
    }

    @Test
    void shouldUseXForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        assertEquals("10.0.0.1", filter.getClientIp(request));
    }

    @Test
    void shouldUseXRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.5");
        assertEquals("10.0.0.5", filter.getClientIp(request));
    }

    @Test
    void shouldExposeCorrectConstants() {
        assertEquals(20, RateLimitFilter.getMaxRequests());
        assertEquals(60_000L, RateLimitFilter.getWindowMs());
    }

    // ===== V3.1 增强：覆盖所有敏感 API 接口 =====

    @ParameterizedTest
    @ValueSource(strings = {
        "POST:/api/emails/send",
        "PUT:/api/emails/config"
    })
    void shouldRateLimitEmailEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "POST:/api/users/create",
        "PUT:/api/users/1",
        "DELETE:/api/users/1"
    })
    void shouldRateLimitUserManagementEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "POST:/api/internships/1/convert",
        "POST:/api/internships/1/terminate",
        "POST:/api/internships/1/extend"
    })
    void shouldRateLimitInternshipSensitiveEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "PUT:/api/members/1/status"
    })
    void shouldRateLimitMemberStatusEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "POST:/api/backups/execute",
        "POST:/api/backups/sync"
    })
    void shouldRateLimitBackupEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "POST:/api/salary/calculate",
        "POST:/api/salary/batch-save",
        "PUT:/api/salary/1"
    })
    void shouldRateLimitSalaryEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "GET:/api/reports/export/members",
        "GET:/api/reports/export/salary",
        "GET:/api/reports/export/points",
        "GET:/api/reports/export/activities",
        "GET:/api/applications/export"
    })
    void shouldRateLimitExportEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "POST:/api/applications/batch-approve",
        "POST:/api/applications/batch-reject",
        "POST:/api/applications/batch-notify-interview"
    })
    void shouldRateLimitBatchOperationEndpoints(String methodAndPath) {
        String[] parts = methodAndPath.split(":", 2);
        assertTrue(filter.isRateLimited(parts[0], parts[1]),
            "Should rate limit: " + methodAndPath);
    }

    @Test
    void shouldNotRateLimitGetRequestsOnUserEndpoints() {
        // GET on user endpoints should NOT be rate limited (only POST/PUT/DELETE are configured)
        assertFalse(filter.isRateLimited("GET", "/api/users/1"),
            "GET on /api/users should not be rate limited");
    }

    @Test
    void shouldRespectMethodSpecificRateLimiting() {
        // POST on salary should be rate limited
        assertTrue(filter.isRateLimited("POST", "/api/salary/calculate"));
        // GET on salary should NOT be rate limited (only POST/PUT configured)
        assertFalse(filter.isRateLimited("GET", "/api/salary/list"));
    }

    @Test
    void shouldUseCustomLimitsFromProperties() {
        RateLimitProperties props = new RateLimitProperties();
        RateLimitProperties.EndpointLimit customLimit = new RateLimitProperties.EndpointLimit();
        customLimit.setMaxRequests(3);
        customLimit.setWindowMs(30_000L);
        props.getEndpoints().put("/api/test/**", customLimit);

        RateLimitFilter customFilter = new RateLimitFilter(props, new ObjectMapper(), null);

        RateLimitFilter.MatchedEndpoint matched = customFilter.findMatchingEndpoint("GET", "/api/test/something");
        assertNotNull(matched);
        assertEquals(3, matched.limit.getMaxRequests());
        assertEquals(30_000L, matched.limit.getWindowMs());
    }

    @Test
    void shouldFallbackToInMemoryWhenRedisUnavailable() {
        assertFalse(filter.isRedisAvailable());

        String key = "test-key";
        assertFalse(filter.isLimitExceeded(key, 2, 60_000L));
        assertFalse(filter.isLimitExceeded(key, 2, 60_000L));
        assertTrue(filter.isLimitExceeded(key, 2, 60_000L));
    }

    @Test
    void shouldEnforceStricterLimitsOnBatchOperations() throws ServletException, IOException {
        // Batch operations have limit of 5
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/applications/batch-approve");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
            assertEquals(200, resp.getStatus());
        }

        // 6th request should be blocked
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/applications/batch-approve");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertEquals(429, response.getStatus());
    }

    @Test
    void shouldEnforceStricterLimitsOnBackupOperations() throws ServletException, IOException {
        // Backup operations have limit of 3
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/backups/execute");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
            assertEquals(200, resp.getStatus());
        }

        // 4th request should be blocked
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/backups/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertEquals(429, response.getStatus());
    }
}
