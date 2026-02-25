package com.pollen.management.property;

import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.PointsService;
import com.pollen.management.service.SalaryConfigService;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for salary distribution range and total invariant.
 *
 * Feature: salary-calculation-rules, Property 8: 薪酬分配范围与总额不变量
 * For any 薪酬池分配结果，每位正式成员的最终迷你币应在配置的 [最低值, 最高值] 范围内，
 * 且所有成员最终迷你币总和不超过薪酬池总额。
 *
 * **Validates: Requirements 4.4, 4.6**
 */
class SalaryRangeAndTotalInvariantPropertyTest {

    private static final int MEMBER_COUNT = 5;

    private SalaryServiceImpl createService(int salaryPoolTotal, int minCoins, int maxCoins) {
        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getSalaryPoolTotal()).thenReturn(salaryPoolTotal);
        when(configService.getMiniCoinsRange()).thenReturn(new int[]{minCoins, maxCoins});

        return new SalaryServiceImpl(
                mock(SalaryRecordRepository.class),
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                configService
        );
    }

    /**
     * Invoke the package-private performanceAdjust method via reflection.
     */
    private List<Integer> invokePerformanceAdjust(SalaryServiceImpl service,
                                                   List<Integer> miniCoinsList) throws Exception {
        Method method = SalaryServiceImpl.class.getDeclaredMethod("performanceAdjust", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, miniCoinsList);
        return result;
    }

    // ========================================================================
    // Property 8: 薪酬分配范围与总额不变量
    // **Validates: Requirements 4.4, 4.6**
    // ========================================================================

    @Property(tries = 100)
    void property8_eachMemberFinalCoinsWithinConfiguredRange(
            @ForAll("adjustedCoinsList") List<Integer> adjustedCoins,
            @ForAll("minMaxRange") int[] range) {

        int minCoins = range[0];
        int maxCoins = range[1];
        int poolTotal = maxCoins * MEMBER_COUNT; // ensure pool is large enough

        SalaryServiceImpl service = createService(poolTotal, minCoins, maxCoins);

        try {
            List<Integer> finalCoins = invokePerformanceAdjust(service, adjustedCoins);

            assertThat(finalCoins).hasSize(MEMBER_COUNT);

            for (int i = 0; i < MEMBER_COUNT; i++) {
                assertThat(finalCoins.get(i))
                        .as("Member %d final coins (%d) should be >= minCoins (%d)",
                                i, finalCoins.get(i), minCoins)
                        .isGreaterThanOrEqualTo(minCoins);
                assertThat(finalCoins.get(i))
                        .as("Member %d final coins (%d) should be <= maxCoins (%d)",
                                i, finalCoins.get(i), maxCoins)
                        .isLessThanOrEqualTo(maxCoins);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke performanceAdjust", e);
        }
    }

    @Property(tries = 100)
    void property8_totalFinalCoinsDoesNotExceedPoolTotal(
            @ForAll("poolAdjustedCoinsAndPool") PoolAdjustedInput input) {

        int minCoins = input.minCoins;
        int maxCoins = input.maxCoins;
        int poolTotal = input.poolTotal;

        SalaryServiceImpl service = createService(poolTotal, minCoins, maxCoins);

        try {
            List<Integer> finalCoins = invokePerformanceAdjust(service, input.adjustedCoins);

            int totalFinal = finalCoins.stream().mapToInt(Integer::intValue).sum();
            assertThat(totalFinal)
                    .as("Sum of final coins (%d) should not exceed pool total (%d)",
                            totalFinal, poolTotal)
                    .isLessThanOrEqualTo(poolTotal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke performanceAdjust", e);
        }
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<List<Integer>> adjustedCoinsList() {
        return Arbitraries.integers().between(50, 600)
                .list().ofSize(MEMBER_COUNT);
    }

    @Provide
    Arbitrary<int[]> minMaxRange() {
        // Generate valid min/max pairs where min <= max
        return Arbitraries.integers().between(100, 300)
                .flatMap(min -> Arbitraries.integers().between(min, min + 300)
                        .map(max -> new int[]{min, max}));
    }

    @Provide
    Arbitrary<PoolAdjustedInput> poolAdjustedCoinsAndPool() {
        // Generate coins that sum to at most poolTotal, with valid min/max range
        // This simulates the output of adjustToPool feeding into performanceAdjust
        return Arbitraries.integers().between(100, 250).flatMap(min ->
                Arbitraries.integers().between(min, min + 200).flatMap(max ->
                        Arbitraries.integers().between(max * MEMBER_COUNT, max * MEMBER_COUNT + 1000).flatMap(pool ->
                                Arbitraries.integers().between(0, pool / MEMBER_COUNT + 100)
                                        .list().ofSize(MEMBER_COUNT)
                                        .filter(coins -> coins.stream().mapToInt(Integer::intValue).sum() <= pool)
                                        .map(coins -> new PoolAdjustedInput(coins, pool, min, max))
                        )
                )
        );
    }

    /**
     * Helper class to bundle pool-adjusted coins with their configuration.
     */
    static class PoolAdjustedInput {
        final List<Integer> adjustedCoins;
        final int poolTotal;
        final int minCoins;
        final int maxCoins;

        PoolAdjustedInput(List<Integer> adjustedCoins, int poolTotal, int minCoins, int maxCoins) {
            this.adjustedCoins = adjustedCoins;
            this.poolTotal = poolTotal;
            this.minCoins = minCoins;
            this.maxCoins = maxCoins;
        }

        @Override
        public String toString() {
            return String.format("PoolAdjustedInput{coins=%s, pool=%d, min=%d, max=%d}",
                    adjustedCoins, poolTotal, minCoins, maxCoins);
        }
    }
}
