package com.pollen.management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
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
        assertFalse(filter.isRateLimited("/api/users/me"));
        assertFalse(filter.isRateLimited("/api/applications"));
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
}
