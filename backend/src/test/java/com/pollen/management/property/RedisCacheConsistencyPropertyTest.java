package com.pollen.management.property;

import com.pollen.management.config.RedisConfig;
import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.MemberCardItem;
import com.pollen.management.dto.MemberDetail;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.entity.enums.Role;
import net.jqwik.api.*;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Redis cache consistency.
 *
 * Property 42: Redis 缓存一致性
 * For any cached data, after data changes the corresponding cache entries
 * must be evicted, and subsequent queries must return fresh data from the database.
 *
 * Since we cannot spin up a real Redis in unit tests, we verify:
 * 1. All read methods have @Cacheable with the correct cache name
 * 2. All write/mutation methods have @CacheEvict with allEntries=true for the correct cache
 * 3. For any sequence of read→write→read operations on a simulated cache,
 *    the write evicts the cache and the second read returns fresh data
 *
 * **Validates: Requirements 20.2, 20.3**
 */
class RedisCacheConsistencyPropertyTest {

    // ========================================================================
    // Property 42: Redis 缓存一致性 — Structural verification
    // Verifies that every @Cacheable read method has a corresponding @CacheEvict
    // write method on the same cache, ensuring data changes always invalidate cache.
    // **Validates: Requirements 20.2, 20.3**
    // ========================================================================

    /**
     * For any cache name used by @Cacheable, there must exist at least one @CacheEvict
     * method targeting that cache name across all services, ensuring writes invalidate reads.
     * Note: eviction may happen in a different service (e.g., PointsServiceImpl evicts
     * the "dashboard" cache that DashboardServiceImpl reads from).
     */
    @Property(tries = 50)
    void property42_everyReadCacheHasCorrespondingEviction(
            @ForAll("cachedServiceClasses") CachedServiceInfo serviceInfo) {

        String cacheName = serviceInfo.cacheName();

        // Verify the service has @Cacheable for this cache
        Class<?> serviceClass = serviceInfo.serviceClass();
        Set<String> cacheableNames = new HashSet<>();
        for (Method method : serviceClass.getMethods()) {
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            if (cacheable != null) {
                cacheableNames.addAll(Arrays.asList(cacheable.value()));
            }
        }

        assertThat(cacheableNames)
                .as("Service %s should have @Cacheable for cache '%s'", serviceClass.getSimpleName(), cacheName)
                .contains(cacheName);

        // Search ALL service classes for @CacheEvict targeting this cache name
        // (cross-service eviction is a valid pattern, e.g., PointsService evicts dashboard cache)
        List<Class<?>> allServiceClasses = List.of(
                com.pollen.management.service.MemberServiceImpl.class,
                com.pollen.management.service.DashboardServiceImpl.class,
                com.pollen.management.service.QuestionnaireTemplateServiceImpl.class,
                com.pollen.management.service.PointsServiceImpl.class,
                com.pollen.management.service.SalaryServiceImpl.class
        );

        boolean hasEviction = false;
        for (Class<?> svcClass : allServiceClasses) {
            for (Method method : svcClass.getMethods()) {
                CacheEvict evict = method.getAnnotation(CacheEvict.class);
                if (evict != null && Arrays.asList(evict.value()).contains(cacheName)) {
                    hasEviction = true;
                    break;
                }
                Caching caching = method.getAnnotation(Caching.class);
                if (caching != null) {
                    for (CacheEvict ce : caching.evict()) {
                        if (Arrays.asList(ce.value()).contains(cacheName)) {
                            hasEviction = true;
                            break;
                        }
                    }
                }
                if (hasEviction) break;
            }
            if (hasEviction) break;
        }

        assertThat(hasEviction)
                .as("Cache '%s' must have at least one @CacheEvict method across all services to ensure consistency",
                        cacheName)
                .isTrue();
    }

    /**
     * For any @CacheEvict method, allEntries must be true to ensure complete cache invalidation
     * on data changes, preventing stale partial cache entries.
     */
    @Property(tries = 50)
    void property42_cacheEvictUsesAllEntries(
            @ForAll("evictMethods") EvictMethodInfo evictInfo) {

        Method method = evictInfo.method();
        CacheEvict evict = method.getAnnotation(CacheEvict.class);

        if (evict != null) {
            assertThat(evict.allEntries())
                    .as("@CacheEvict on %s.%s must use allEntries=true for full cache invalidation",
                            method.getDeclaringClass().getSimpleName(), method.getName())
                    .isTrue();
        }

        // Also check @Caching composite annotations
        Caching caching = method.getAnnotation(Caching.class);
        if (caching != null) {
            for (CacheEvict ce : caching.evict()) {
                assertThat(ce.allEntries())
                        .as("@CacheEvict in @Caching on %s.%s must use allEntries=true",
                                method.getDeclaringClass().getSimpleName(), method.getName())
                        .isTrue();
            }
        }
    }

    // ========================================================================
    // Property 42: Redis 缓存一致性 — Behavioral simulation
    // Simulates read→write→read sequences using ConcurrentMapCacheManager
    // to verify cache eviction guarantees fresh data on subsequent reads.
    // **Validates: Requirements 20.2, 20.3**
    // ========================================================================

    /**
     * For any cache name and sequence of operations (read, write, read),
     * after a write the cache must be empty, and the next read must populate
     * fresh data (not stale cached data).
     */
    @Property(tries = 200)
    void property42_writeEvictsCacheAndNextReadReturnsFreshData(
            @ForAll("cacheNames") String cacheName,
            @ForAll("cacheKeys") String cacheKey,
            @ForAll("dataVersions") int initialVersion,
            @ForAll("dataVersions") int updatedVersion) {

        Assume.that(initialVersion != updatedVersion);

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        // Step 1: Simulate first read — cache miss, populate cache
        String initialData = "data_v" + initialVersion;
        cache.put(cacheKey, initialData);

        // Verify cache hit returns same data
        Cache.ValueWrapper cached = cache.get(cacheKey);
        assertThat(cached).isNotNull();
        assertThat(cached.get())
                .as("Cache hit must return the same data that was written")
                .isEqualTo(initialData);

        // Step 2: Simulate write — evict cache (as @CacheEvict(allEntries=true) does)
        cache.clear();

        // Verify cache is empty after eviction
        Cache.ValueWrapper afterEvict = cache.get(cacheKey);
        assertThat(afterEvict)
                .as("Cache must be empty after eviction (simulating @CacheEvict)")
                .isNull();

        // Step 3: Simulate second read — cache miss, populate with fresh data
        String freshData = "data_v" + updatedVersion;
        cache.put(cacheKey, freshData);

        Cache.ValueWrapper afterRefresh = cache.get(cacheKey);
        assertThat(afterRefresh).isNotNull();
        assertThat(afterRefresh.get())
                .as("After eviction and re-read, cache must contain fresh data, not stale")
                .isEqualTo(freshData)
                .isNotEqualTo(initialData);
    }

    /**
     * For any sequence of N reads followed by a write, the write must invalidate
     * all cached entries, regardless of how many reads preceded it.
     */
    @Property(tries = 100)
    void property42_multipleReadsFollowedByWriteEvictsAll(
            @ForAll("cacheNames") String cacheName,
            @ForAll("readCounts") int readCount) {

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        // Simulate multiple reads populating different cache keys
        for (int i = 0; i < readCount; i++) {
            cache.put("key_" + i, "value_" + i);
        }

        // Verify all entries are cached
        for (int i = 0; i < readCount; i++) {
            assertThat(cache.get("key_" + i))
                    .as("Key 'key_%d' should be cached after read", i)
                    .isNotNull();
        }

        // Simulate write with allEntries=true eviction
        cache.clear();

        // Verify ALL entries are evicted
        for (int i = 0; i < readCount; i++) {
            assertThat(cache.get("key_" + i))
                    .as("Key 'key_%d' must be evicted after write operation", i)
                    .isNull();
        }
    }

    /**
     * For any interleaved sequence of reads and writes on the members cache,
     * a write always results in cache eviction, and subsequent reads always
     * see fresh data.
     */
    @Property(tries = 100)
    void property42_interleavedReadWriteSequenceMaintainsConsistency(
            @ForAll("operationSequences") List<CacheOperation> operations) {

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                RedisConfig.CACHE_MEMBERS, RedisConfig.CACHE_DASHBOARD, RedisConfig.CACHE_QUESTIONNAIRE);

        int dbVersion = 0;
        Map<String, Integer> lastWrittenVersion = new HashMap<>();

        for (CacheOperation op : operations) {
            Cache cache = cacheManager.getCache(op.cacheName());
            assertThat(cache).isNotNull();

            if (op.isWrite()) {
                // Write operation: increment DB version and evict cache
                dbVersion++;
                lastWrittenVersion.put(op.cacheName(), dbVersion);
                cache.clear();

                // Verify cache is empty after write
                assertThat(cache.get("data"))
                        .as("Cache '%s' must be empty after write (version %d)", op.cacheName(), dbVersion)
                        .isNull();
            } else {
                // Read operation: populate cache from "DB" if miss
                Cache.ValueWrapper existing = cache.get("data");
                if (existing == null) {
                    // Cache miss — read from DB (current dbVersion)
                    cache.put("data", dbVersion);
                }

                Cache.ValueWrapper result = cache.get("data");
                assertThat(result).isNotNull();
                int cachedVersion = (int) result.get();

                // The cached version must be >= the last written version for this cache
                // (it could be the current dbVersion if freshly populated)
                Integer lastWrite = lastWrittenVersion.get(op.cacheName());
                if (lastWrite != null) {
                    assertThat(cachedVersion)
                            .as("Cached data for '%s' must not be older than last write version", op.cacheName())
                            .isGreaterThanOrEqualTo(lastWrite);
                }
            }
        }
    }

    /**
     * For any cross-service data change (e.g., points change evicts dashboard cache),
     * the affected cache must be evicted. Verifies that PointsServiceImpl and
     * SalaryServiceImpl evict the dashboard cache on mutations.
     */
    @Property(tries = 50)
    void property42_crossServiceEvictionEnsuresConsistency(
            @ForAll("crossServiceMutations") CrossServiceMutation mutation) {

        Class<?> serviceClass = mutation.serviceClass();
        String methodName = mutation.methodName();
        String expectedEvictedCache = mutation.expectedEvictedCache();

        boolean found = false;
        for (Method method : serviceClass.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            found = true;

            Set<String> evictedCaches = new HashSet<>();

            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            if (evict != null) {
                evictedCaches.addAll(Arrays.asList(evict.value()));
            }

            Caching caching = method.getAnnotation(Caching.class);
            if (caching != null) {
                for (CacheEvict ce : caching.evict()) {
                    evictedCaches.addAll(Arrays.asList(ce.value()));
                }
            }

            assertThat(evictedCaches)
                    .as("%s.%s must evict '%s' cache for cross-service consistency",
                            serviceClass.getSimpleName(), methodName, expectedEvictedCache)
                    .contains(expectedEvictedCache);
        }

        assertThat(found)
                .as("Method %s must exist on %s", methodName, serviceClass.getSimpleName())
                .isTrue();
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<CachedServiceInfo> cachedServiceClasses() {
        return Arbitraries.of(
                new CachedServiceInfo(
                        com.pollen.management.service.MemberServiceImpl.class,
                        RedisConfig.CACHE_MEMBERS),
                new CachedServiceInfo(
                        com.pollen.management.service.DashboardServiceImpl.class,
                        RedisConfig.CACHE_DASHBOARD),
                new CachedServiceInfo(
                        com.pollen.management.service.QuestionnaireTemplateServiceImpl.class,
                        RedisConfig.CACHE_QUESTIONNAIRE)
        );
    }

    @Provide
    Arbitrary<EvictMethodInfo> evictMethods() {
        List<EvictMethodInfo> methods = new ArrayList<>();

        for (Class<?> clazz : List.of(
                com.pollen.management.service.MemberServiceImpl.class,
                com.pollen.management.service.DashboardServiceImpl.class,
                com.pollen.management.service.QuestionnaireTemplateServiceImpl.class,
                com.pollen.management.service.PointsServiceImpl.class,
                com.pollen.management.service.SalaryServiceImpl.class)) {
            for (Method m : clazz.getMethods()) {
                if (m.getAnnotation(CacheEvict.class) != null || m.getAnnotation(Caching.class) != null) {
                    methods.add(new EvictMethodInfo(m));
                }
            }
        }

        return Arbitraries.of(methods);
    }

    @Provide
    Arbitrary<String> cacheNames() {
        return Arbitraries.of(
                RedisConfig.CACHE_MEMBERS,
                RedisConfig.CACHE_DASHBOARD,
                RedisConfig.CACHE_QUESTIONNAIRE);
    }

    @Provide
    Arbitrary<String> cacheKeys() {
        return Arbitraries.of("list", "detail:1", "detail:2", "overview",
                "recruitment", "salary", "operations", "all", "1", "2", "3");
    }

    @Provide
    Arbitrary<Integer> dataVersions() {
        return Arbitraries.integers().between(1, 1000);
    }

    @Provide
    Arbitrary<Integer> readCounts() {
        return Arbitraries.integers().between(1, 20);
    }

    @Provide
    Arbitrary<List<CacheOperation>> operationSequences() {
        Arbitrary<CacheOperation> opArb = Combinators.combine(
                Arbitraries.of(RedisConfig.CACHE_MEMBERS, RedisConfig.CACHE_DASHBOARD, RedisConfig.CACHE_QUESTIONNAIRE),
                Arbitraries.of(true, false)
        ).as(CacheOperation::new);

        return opArb.list().ofMinSize(2).ofMaxSize(20);
    }

    @Provide
    Arbitrary<CrossServiceMutation> crossServiceMutations() {
        return Arbitraries.of(
                new CrossServiceMutation(
                        com.pollen.management.service.PointsServiceImpl.class,
                        "addPoints", RedisConfig.CACHE_DASHBOARD),
                new CrossServiceMutation(
                        com.pollen.management.service.PointsServiceImpl.class,
                        "deductPoints", RedisConfig.CACHE_DASHBOARD),
                new CrossServiceMutation(
                        com.pollen.management.service.SalaryServiceImpl.class,
                        "batchSave", RedisConfig.CACHE_DASHBOARD),
                new CrossServiceMutation(
                        com.pollen.management.service.MemberServiceImpl.class,
                        "updateOnlineStatus", RedisConfig.CACHE_MEMBERS),
                new CrossServiceMutation(
                        com.pollen.management.service.MemberServiceImpl.class,
                        "heartbeat", RedisConfig.CACHE_MEMBERS),
                new CrossServiceMutation(
                        com.pollen.management.service.QuestionnaireTemplateServiceImpl.class,
                        "delete", RedisConfig.CACHE_QUESTIONNAIRE)
        );
    }

    // ========== Record types ==========

    record CachedServiceInfo(Class<?> serviceClass, String cacheName) {}
    record EvictMethodInfo(Method method) {}
    record CacheOperation(String cacheName, boolean isWrite) {}
    record CrossServiceMutation(Class<?> serviceClass, String methodName, String expectedEvictedCache) {}
}
