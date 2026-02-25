package com.pollen.management.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.entity.SalaryConfig;
import com.pollen.management.repository.SalaryConfigRepository;
import com.pollen.management.service.SalaryConfigServiceImpl;
import net.jqwik.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for SalaryConfigService round-trip consistency.
 *
 * Feature: salary-calculation-rules, Property 1: 配置读写一致性（Round-Trip）
 * For any 合法的配置键值对，保存到 Config_Manager 后再读取，应返回与保存时相同的值。
 *
 * **Validates: Requirements 1.2**
 */
class SalaryConfigRoundTripPropertyTest {

    // ========================================================================
    // Property 1: 配置读写一致性（Round-Trip）
    // For any valid config key-value pair, saving via saveConfig then reading
    // via getConfigValue should return the same value that was saved.
    // **Validates: Requirements 1.2**
    // ========================================================================

    @Property(tries = 100)
    void property1_saveConfigThenReadReturnsOriginalValue(
            @ForAll("validSalaryPoolTotal") int salaryPoolTotal,
            @ForAll("validFormalMemberCount") int formalMemberCount,
            @ForAll("validPointsToCoinsRatio") int pointsToCoinsRatio) {

        // In-memory store to simulate repository
        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        // Build a valid config map that passes validation:
        // base_allocation * formalMemberCount <= salaryPoolTotal
        int baseAllocation = salaryPoolTotal / formalMemberCount;
        // mini_coins_min <= mini_coins_max
        int miniCoinsMin = 100;
        int miniCoinsMax = Math.max(miniCoinsMin, baseAllocation);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("salary_pool_total", String.valueOf(salaryPoolTotal));
        configMap.put("formal_member_count", String.valueOf(formalMemberCount));
        configMap.put("base_allocation", String.valueOf(baseAllocation));
        configMap.put("mini_coins_min", String.valueOf(miniCoinsMin));
        configMap.put("mini_coins_max", String.valueOf(miniCoinsMax));
        configMap.put("points_to_coins_ratio", String.valueOf(pointsToCoinsRatio));

        // Save
        service.saveConfig(configMap);

        // Read back each key and verify round-trip
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String readValue = service.getConfigValue(entry.getKey(), "NOT_FOUND");
            assertThat(readValue)
                    .as("Config key '%s' should round-trip: saved '%s', read '%s'",
                            entry.getKey(), entry.getValue(), readValue)
                    .isEqualTo(entry.getValue());
        }
    }

    @Property(tries = 100)
    void property1_saveConfigThenReadIntReturnsOriginalValue(
            @ForAll("validSalaryPoolTotal") int salaryPoolTotal,
            @ForAll("validFormalMemberCount") int formalMemberCount) {

        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        int baseAllocation = salaryPoolTotal / formalMemberCount;
        int miniCoinsMin = 100;
        int miniCoinsMax = Math.max(miniCoinsMin, baseAllocation);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("salary_pool_total", String.valueOf(salaryPoolTotal));
        configMap.put("formal_member_count", String.valueOf(formalMemberCount));
        configMap.put("base_allocation", String.valueOf(baseAllocation));
        configMap.put("mini_coins_min", String.valueOf(miniCoinsMin));
        configMap.put("mini_coins_max", String.valueOf(miniCoinsMax));

        service.saveConfig(configMap);

        // Verify integer read round-trip
        assertThat(service.getIntConfig("salary_pool_total", -1))
                .as("getIntConfig should return saved salary_pool_total")
                .isEqualTo(salaryPoolTotal);
        assertThat(service.getIntConfig("formal_member_count", -1))
                .as("getIntConfig should return saved formal_member_count")
                .isEqualTo(formalMemberCount);
        assertThat(service.getIntConfig("base_allocation", -1))
                .as("getIntConfig should return saved base_allocation")
                .isEqualTo(baseAllocation);
    }

    @Property(tries = 100)
    void property1_saveRotationThresholdsThenReadReturnsOriginalValues(
            @ForAll("validThreshold") int promotionThreshold,
            @ForAll("validThreshold") int demotionThreshold,
            @ForAll("validThreshold") int dismissalThreshold) {

        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        Map<String, String> configMap = new HashMap<>();
        configMap.put("promotion_points_threshold", String.valueOf(promotionThreshold));
        configMap.put("demotion_salary_threshold", String.valueOf(demotionThreshold));
        configMap.put("dismissal_points_threshold", String.valueOf(dismissalThreshold));

        service.saveConfig(configMap);

        // Read back via getConfigValue
        assertThat(service.getConfigValue("promotion_points_threshold", "NOT_FOUND"))
                .isEqualTo(String.valueOf(promotionThreshold));
        assertThat(service.getConfigValue("demotion_salary_threshold", "NOT_FOUND"))
                .isEqualTo(String.valueOf(demotionThreshold));
        assertThat(service.getConfigValue("dismissal_points_threshold", "NOT_FOUND"))
                .isEqualTo(String.valueOf(dismissalThreshold));

        // Also verify via getRotationThresholds convenience method
        var thresholds = service.getRotationThresholds();
        assertThat(thresholds.getPromotionPointsThreshold()).isEqualTo(promotionThreshold);
        assertThat(thresholds.getDemotionSalaryThreshold()).isEqualTo(demotionThreshold);
        assertThat(thresholds.getDismissalPointsThreshold()).isEqualTo(dismissalThreshold);
    }

    @Property(tries = 100)
    void property1_overwriteConfigPreservesLatestValue(
            @ForAll("validThreshold") int firstThreshold,
            @ForAll("validThreshold") int secondThreshold) {

        Map<String, SalaryConfig> store = new HashMap<>();
        SalaryConfigRepository repo = createMockRepository(store);
        SalaryConfigServiceImpl service = new SalaryConfigServiceImpl(repo, new ObjectMapper());

        // Use rotation thresholds for overwrite test since they have no cross-field validation
        // Save first value
        Map<String, String> firstConfig = new HashMap<>();
        firstConfig.put("promotion_points_threshold", String.valueOf(firstThreshold));
        service.saveConfig(firstConfig);

        // Save second value (overwrite)
        Map<String, String> secondConfig = new HashMap<>();
        secondConfig.put("promotion_points_threshold", String.valueOf(secondThreshold));
        service.saveConfig(secondConfig);

        // Should read the latest value
        String readValue = service.getConfigValue("promotion_points_threshold", "NOT_FOUND");
        assertThat(readValue)
                .as("After overwrite, should return the latest saved value")
                .isEqualTo(String.valueOf(secondThreshold));
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<Integer> validSalaryPoolTotal() {
        return Arbitraries.integers().between(500, 50000);
    }

    @Provide
    Arbitrary<Integer> validFormalMemberCount() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<Integer> validPointsToCoinsRatio() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<Integer> validThreshold() {
        return Arbitraries.integers().between(0, 1000);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Creates a mock SalaryConfigRepository backed by an in-memory HashMap,
     * simulating real save/find behavior for round-trip testing.
     */
    private SalaryConfigRepository createMockRepository(Map<String, SalaryConfig> store) {
        SalaryConfigRepository repo = mock(SalaryConfigRepository.class);

        // findByConfigKey: look up from store
        when(repo.findByConfigKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return Optional.ofNullable(store.get(key));
        });

        // findAll: return all values from store
        when(repo.findAll()).thenAnswer(invocation ->
                new ArrayList<>(store.values()));

        // save: persist to store and return
        when(repo.save(any(SalaryConfig.class))).thenAnswer(invocation -> {
            SalaryConfig config = invocation.getArgument(0);
            store.put(config.getConfigKey(), config);
            return config;
        });

        return repo;
    }
}
