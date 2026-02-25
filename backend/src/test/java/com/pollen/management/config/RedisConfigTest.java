package com.pollen.management.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    private RedisConfig redisConfig;
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
        connectionFactory = mock(RedisConnectionFactory.class);
    }

    @Test
    void cacheManager_shouldNotBeNull() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        assertNotNull(cacheManager);
    }

    @Test
    void cacheManager_shouldContainMembersCache() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        cacheManager.initializeCaches();
        assertNotNull(cacheManager.getCache(RedisConfig.CACHE_MEMBERS));
    }

    @Test
    void cacheManager_shouldContainDashboardCache() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        cacheManager.initializeCaches();
        assertNotNull(cacheManager.getCache(RedisConfig.CACHE_DASHBOARD));
    }

    @Test
    void cacheManager_shouldContainQuestionnaireCache() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        cacheManager.initializeCaches();
        assertNotNull(cacheManager.getCache(RedisConfig.CACHE_QUESTIONNAIRE));
    }

    @Test
    void membersTtl_shouldBeFiveMinutes() {
        assertEquals(Duration.ofMinutes(5), RedisConfig.MEMBERS_TTL);
    }

    @Test
    void dashboardTtl_shouldBeTenMinutes() {
        assertEquals(Duration.ofMinutes(10), RedisConfig.DASHBOARD_TTL);
    }

    @Test
    void questionnaireTtl_shouldBeThirtyMinutes() {
        assertEquals(Duration.ofMinutes(30), RedisConfig.QUESTIONNAIRE_TTL);
    }

    @Test
    void cacheConstants_shouldHaveCorrectNames() {
        assertEquals("members", RedisConfig.CACHE_MEMBERS);
        assertEquals("dashboard", RedisConfig.CACHE_DASHBOARD);
        assertEquals("questionnaire", RedisConfig.CACHE_QUESTIONNAIRE);
    }

    @Test
    void cacheManager_shouldBeTransactionAware() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        assertTrue(cacheManager.isTransactionAware());
    }
}
