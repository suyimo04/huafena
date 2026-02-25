package com.pollen.management.service;

import com.pollen.management.config.RedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证缓存注解正确配置在各 Service 实现类的方法上。
 * Validates: Requirements 20.2, 20.3
 */
class CacheAnnotationTest {

    // ── MemberServiceImpl ──────────────────────────────────────────

    @Nested
    @DisplayName("MemberServiceImpl 缓存注解")
    class MemberServiceCacheTests {

        @Test
        @DisplayName("listMembers 应有 @Cacheable(members)")
        void listMembers_shouldHaveCacheable() throws Exception {
            Method method = MemberServiceImpl.class.getMethod("listMembers");
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "listMembers 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_MEMBERS}, cacheable.value());
        }

        @Test
        @DisplayName("getMemberDetail 应有 @Cacheable(members)")
        void getMemberDetail_shouldHaveCacheable() throws Exception {
            Method method = MemberServiceImpl.class.getMethod("getMemberDetail", Long.class);
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "getMemberDetail 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_MEMBERS}, cacheable.value());
        }

        @Test
        @DisplayName("updateOnlineStatus 应有 @CacheEvict(members)")
        void updateOnlineStatus_shouldHaveCacheEvict() throws Exception {
            Method method = MemberServiceImpl.class.getMethod("updateOnlineStatus", Long.class,
                    com.pollen.management.entity.enums.OnlineStatus.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "updateOnlineStatus 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_MEMBERS}, evict.value());
            assertTrue(evict.allEntries(), "updateOnlineStatus @CacheEvict 应设置 allEntries=true");
        }

        @Test
        @DisplayName("heartbeat 应有 @CacheEvict(members)")
        void heartbeat_shouldHaveCacheEvict() throws Exception {
            Method method = MemberServiceImpl.class.getMethod("heartbeat", Long.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "heartbeat 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_MEMBERS}, evict.value());
            assertTrue(evict.allEntries(), "heartbeat @CacheEvict 应设置 allEntries=true");
        }
    }

    // ── DashboardServiceImpl ───────────────────────────────────────

    @Nested
    @DisplayName("DashboardServiceImpl 缓存注解")
    class DashboardServiceCacheTests {

        @Test
        @DisplayName("getDashboardStats 应有 @Cacheable(dashboard)")
        void getDashboardStats_shouldHaveCacheable() throws Exception {
            Method method = DashboardServiceImpl.class.getMethod("getDashboardStats");
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "getDashboardStats 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_DASHBOARD}, cacheable.value());
        }

        @Test
        @DisplayName("getRecruitmentStats 应有 @Cacheable(dashboard)")
        void getRecruitmentStats_shouldHaveCacheable() throws Exception {
            Method method = DashboardServiceImpl.class.getMethod("getRecruitmentStats");
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "getRecruitmentStats 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_DASHBOARD}, cacheable.value());
        }

        @Test
        @DisplayName("getSalaryStats 应有 @Cacheable(dashboard)")
        void getSalaryStats_shouldHaveCacheable() throws Exception {
            Method method = DashboardServiceImpl.class.getMethod("getSalaryStats");
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "getSalaryStats 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_DASHBOARD}, cacheable.value());
        }

        @Test
        @DisplayName("getOperationsData 应有 @Cacheable(dashboard)")
        void getOperationsData_shouldHaveCacheable() throws Exception {
            Method method = DashboardServiceImpl.class.getMethod("getOperationsData");
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "getOperationsData 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_DASHBOARD}, cacheable.value());
        }
    }

    // ── QuestionnaireTemplateServiceImpl ────────────────────────────

    @Nested
    @DisplayName("QuestionnaireTemplateServiceImpl 缓存注解")
    class QuestionnaireServiceCacheTests {

        @Test
        @DisplayName("getById 应有 @Cacheable(questionnaire)")
        void getById_shouldHaveCacheable() throws Exception {
            Method method = QuestionnaireTemplateServiceImpl.class.getMethod("getById", Long.class);
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "getById 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_QUESTIONNAIRE}, cacheable.value());
        }

        @Test
        @DisplayName("listAll 应有 @Cacheable(questionnaire)")
        void listAll_shouldHaveCacheable() throws Exception {
            Method method = QuestionnaireTemplateServiceImpl.class.getMethod("listAll");
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            assertNotNull(cacheable, "listAll 缺少 @Cacheable");
            assertArrayEquals(new String[]{RedisConfig.CACHE_QUESTIONNAIRE}, cacheable.value());
        }

        @Test
        @DisplayName("create 应有 @CacheEvict(questionnaire)")
        void create_shouldHaveCacheEvict() throws Exception {
            Method method = QuestionnaireTemplateServiceImpl.class.getMethod("create",
                    com.pollen.management.dto.CreateTemplateRequest.class, Long.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "create 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_QUESTIONNAIRE}, evict.value());
            assertTrue(evict.allEntries());
        }

        @Test
        @DisplayName("update 应有 @CacheEvict(questionnaire)")
        void update_shouldHaveCacheEvict() throws Exception {
            Method method = QuestionnaireTemplateServiceImpl.class.getMethod("update",
                    Long.class, com.pollen.management.dto.UpdateTemplateRequest.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "update 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_QUESTIONNAIRE}, evict.value());
            assertTrue(evict.allEntries());
        }

        @Test
        @DisplayName("delete 应有 @CacheEvict(questionnaire)")
        void delete_shouldHaveCacheEvict() throws Exception {
            Method method = QuestionnaireTemplateServiceImpl.class.getMethod("delete", Long.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "delete 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_QUESTIONNAIRE}, evict.value());
            assertTrue(evict.allEntries());
        }

        @Test
        @DisplayName("publish 应有 @CacheEvict(questionnaire)")
        void publish_shouldHaveCacheEvict() throws Exception {
            Method method = QuestionnaireTemplateServiceImpl.class.getMethod("publish", Long.class, Long.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "publish 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_QUESTIONNAIRE}, evict.value());
            assertTrue(evict.allEntries());
        }
    }

    // ── Cross-service cache eviction ───────────────────────────────

    @Nested
    @DisplayName("跨服务缓存清除")
    class CrossServiceCacheEvictionTests {

        @Test
        @DisplayName("PointsServiceImpl.addPoints 应清除 dashboard 缓存")
        void addPoints_shouldEvictDashboardCache() throws Exception {
            Method method = PointsServiceImpl.class.getMethod("addPoints", Long.class,
                    com.pollen.management.entity.enums.PointsType.class, int.class, String.class);
            Caching caching = method.getAnnotation(Caching.class);
            assertNotNull(caching, "addPoints 缺少 @Caching");
            boolean hasDashboardEvict = false;
            for (CacheEvict evict : caching.evict()) {
                for (String v : evict.value()) {
                    if (RedisConfig.CACHE_DASHBOARD.equals(v)) {
                        hasDashboardEvict = true;
                    }
                }
            }
            assertTrue(hasDashboardEvict, "addPoints 应清除 dashboard 缓存");
        }

        @Test
        @DisplayName("PointsServiceImpl.deductPoints 应清除 dashboard 缓存")
        void deductPoints_shouldEvictDashboardCache() throws Exception {
            Method method = PointsServiceImpl.class.getMethod("deductPoints", Long.class,
                    com.pollen.management.entity.enums.PointsType.class, int.class, String.class);
            Caching caching = method.getAnnotation(Caching.class);
            assertNotNull(caching, "deductPoints 缺少 @Caching");
            boolean hasDashboardEvict = false;
            for (CacheEvict evict : caching.evict()) {
                for (String v : evict.value()) {
                    if (RedisConfig.CACHE_DASHBOARD.equals(v)) {
                        hasDashboardEvict = true;
                    }
                }
            }
            assertTrue(hasDashboardEvict, "deductPoints 应清除 dashboard 缓存");
        }

        @Test
        @DisplayName("SalaryServiceImpl.batchSave 应清除 dashboard 缓存")
        void batchSave_shouldEvictDashboardCache() throws Exception {
            Method method = SalaryServiceImpl.class.getMethod("batchSave", java.util.List.class);
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertNotNull(evict, "batchSave 缺少 @CacheEvict");
            assertArrayEquals(new String[]{RedisConfig.CACHE_DASHBOARD}, evict.value());
            assertTrue(evict.allEntries());
        }
    }
}
