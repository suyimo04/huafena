package com.pollen.management.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.entity.SalaryConfig;
import com.pollen.management.repository.SalaryConfigRepository;
import com.pollen.management.service.SalaryConfigServiceImpl;
import net.jqwik.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for SalaryConfigService configuration validation.
 *
 * Feature: salary-calculation-rules, Property 2: 配置参数校验拒绝非法组合
 * For any 配置参数组合，若个人最低迷你币大于个人最高迷你币，或基准分配额乘以正式成员数量超过薪酬池总额，
 * 或流转阈值为负数，则 Config_Manager 应拒绝保存。
 *
 * **Validates: Requirements 1.4, 1.5, 6.3**
 */
class SalaryConfigValidationPropertyTest {

    // ========================================================================
    // Sub-property 2a: mini_coins_min > mini_coins_max → IllegalArgumentException
    // **Validates: Requirements 1.4**
    // ========================================================================

    @Property(tries = 100)
    void property2a_rejectsMinGreaterThanMax(
            @ForAll("miniCoinsMinGreaterThanMax") int[] minMaxPair) {

        int miniCoinsMin = minMaxPair[0];
        int miniCoinsMax = minMaxPair[1];

        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        // Use valid values for other fields so only the min>max violation triggers
        int salaryPoolTotal = 5000;
        int formalMemberCount = 1;
        int baseAllocation = 100;

        Map<String, String> configMap = new HashMap<>();
        configMap.put("mini_coins_min", String.valueOf(miniCoinsMin));
        configMap.put("mini_coins_max", String.valueOf(miniCoinsMax));
        configMap.put("salary_pool_total", String.valueOf(salaryPoolTotal));
        configMap.put("formal_member_count", String.valueOf(formalMemberCount));
        configMap.put("base_allocation", String.valueOf(baseAllocation));
        configMap.put("promotion_points_threshold", "100");
        configMap.put("demotion_salary_threshold", "150");
        configMap.put("dismissal_points_threshold", "100");

        assertThatThrownBy(() -> service.saveConfig(configMap))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("个人最低迷你币");
    }

    // ========================================================================
    // Sub-property 2b: base_allocation × formal_member_count > salary_pool_total
    //                   → IllegalArgumentException
    // **Validates: Requirements 1.5**
    // ========================================================================

    @Property(tries = 100)
    void property2b_rejectsAllocationExceedingPool(
            @ForAll("allocationExceedsPool") int[] triple) {

        int baseAllocation = triple[0];
        int formalMemberCount = triple[1];
        int salaryPoolTotal = triple[2];

        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        Map<String, String> configMap = new HashMap<>();
        configMap.put("base_allocation", String.valueOf(baseAllocation));
        configMap.put("formal_member_count", String.valueOf(formalMemberCount));
        configMap.put("salary_pool_total", String.valueOf(salaryPoolTotal));
        // Valid min/max so this validation doesn't trigger
        configMap.put("mini_coins_min", "100");
        configMap.put("mini_coins_max", "500");
        configMap.put("promotion_points_threshold", "100");
        configMap.put("demotion_salary_threshold", "150");
        configMap.put("dismissal_points_threshold", "100");

        assertThatThrownBy(() -> service.saveConfig(configMap))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("超过薪酬池总额");
    }

    // ========================================================================
    // Sub-property 2c: negative rotation thresholds → IllegalArgumentException
    // **Validates: Requirements 6.3**
    // ========================================================================

    @Property(tries = 100)
    void property2c_rejectsNegativeRotationThresholds(
            @ForAll("negativeThresholdConfig") Map<String, String> thresholdConfig) {

        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        // Start with valid base config
        Map<String, String> configMap = new HashMap<>();
        configMap.put("salary_pool_total", "5000");
        configMap.put("formal_member_count", "1");
        configMap.put("base_allocation", "100");
        configMap.put("mini_coins_min", "100");
        configMap.put("mini_coins_max", "500");
        configMap.put("promotion_points_threshold", "100");
        configMap.put("demotion_salary_threshold", "150");
        configMap.put("dismissal_points_threshold", "100");

        // Override with the negative threshold(s)
        configMap.putAll(thresholdConfig);

        assertThatThrownBy(() -> service.saveConfig(configMap))
                .isInstanceOf(IllegalArgumentException.class)
                .message().satisfiesAnyOf(
                        msg -> org.assertj.core.api.Assertions.assertThat(msg).contains("不能为负数")
                );
    }

    // ========================================================================
    // Providers
    // ========================================================================

    /**
     * Generates [min, max] pairs where min > max (strictly).
     */
    @Provide
    Arbitrary<int[]> miniCoinsMinGreaterThanMax() {
        return Arbitraries.integers().between(1, 10000).flatMap(max ->
                Arbitraries.integers().between(max + 1, max + 5000).map(min ->
                        new int[]{min, max}
                )
        );
    }

    /**
     * Generates [baseAllocation, formalMemberCount, salaryPoolTotal] where
     * baseAllocation * formalMemberCount > salaryPoolTotal.
     */
    @Provide
    Arbitrary<int[]> allocationExceedsPool() {
        return Arbitraries.integers().between(100, 5000).flatMap(baseAllocation ->
                Arbitraries.integers().between(1, 20).flatMap(formalMemberCount -> {
                    long product = (long) baseAllocation * formalMemberCount;
                    // salaryPoolTotal must be less than product
                    int maxPool = (int) Math.min(product - 1, Integer.MAX_VALUE);
                    if (maxPool < 1) maxPool = 1;
                    int finalMaxPool = maxPool;
                    return Arbitraries.integers().between(1, finalMaxPool).map(salaryPoolTotal ->
                            new int[]{baseAllocation, formalMemberCount, salaryPoolTotal}
                    );
                })
        );
    }

    /**
     * Generates a config map with at least one negative rotation threshold.
     * Randomly picks which threshold(s) to make negative.
     */
    @Provide
    Arbitrary<Map<String, String>> negativeThresholdConfig() {
        String[] keys = {
                "promotion_points_threshold",
                "demotion_salary_threshold",
                "dismissal_points_threshold"
        };

        return Arbitraries.integers().between(0, 6).flatMap(selector -> {
            // selector 1-3: single negative key, 4-6: two keys, 0: all three
            Arbitrary<Integer> negativeValue = Arbitraries.integers().between(-10000, -1);

            if (selector == 0) {
                // All three negative
                return negativeValue.flatMap(v1 ->
                        negativeValue.flatMap(v2 ->
                                negativeValue.map(v3 -> {
                                    Map<String, String> map = new HashMap<>();
                                    map.put(keys[0], String.valueOf(v1));
                                    map.put(keys[1], String.valueOf(v2));
                                    map.put(keys[2], String.valueOf(v3));
                                    return map;
                                })
                        )
                );
            } else {
                // Single negative key
                int keyIndex = (selector - 1) % 3;
                return negativeValue.map(v -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(keys[keyIndex], String.valueOf(v));
                    return map;
                });
            }
        });
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private SalaryConfigRepository createMockRepository(Map<String, SalaryConfig> store) {
        SalaryConfigRepository repo = mock(SalaryConfigRepository.class);

        when(repo.findByConfigKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return Optional.ofNullable(store.get(key));
        });

        when(repo.findAll()).thenAnswer(invocation ->
                new ArrayList<>(store.values()));

        when(repo.save(any(SalaryConfig.class))).thenAnswer(invocation -> {
            SalaryConfig config = invocation.getArgument(0);
            store.put(config.getConfigKey(), config);
            return config;
        });

        return repo;
    }
}
