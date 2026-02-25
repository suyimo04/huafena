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
 * Property-based tests for salary pool distribution proportionality.
 *
 * Feature: salary-calculation-rules, Property 7: 薪酬池分配比例保持性
 * For any 一组正式成员的原始迷你币列表，若总和超过薪酬池总额，则调整后各成员迷你币的相对比例
 * 应与原始比例一致（在整数舍入误差范围内）；若总和未超过薪酬池总额，则各成员迷你币应保持不变。
 *
 * **Validates: Requirements 4.2, 4.3**
 */
class SalaryPoolDistributionPropertyTest {

    private static final int MEMBER_COUNT = 5;

    private SalaryServiceImpl createService(int salaryPoolTotal) {
        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getSalaryPoolTotal()).thenReturn(salaryPoolTotal);

        return new SalaryServiceImpl(
                mock(SalaryRecordRepository.class),
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                configService
        );
    }

    /**
     * Invoke the package-private adjustToPool method via reflection.
     */
    private List<Integer> invokeAdjustToPool(SalaryServiceImpl service,
                                              List<Integer> rawMiniCoinsList,
                                              int totalRawMiniCoins) throws Exception {
        Method method = SalaryServiceImpl.class.getDeclaredMethod(
                "adjustToPool", List.class, int.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, rawMiniCoinsList, totalRawMiniCoins);
        return result;
    }

    // ========================================================================
    // Property 7: 薪酬池分配比例保持性
    // **Validates: Requirements 4.2, 4.3**
    // ========================================================================

    @Property(tries = 100)
    void property7_whenSumExceedsPool_adjustedCoinsMaintainProportionalRatios(
            @ForAll("positiveRawCoinsList") List<Integer> rawCoins,
            @ForAll("salaryPoolTotal") int poolTotal) {

        int totalRaw = rawCoins.stream().mapToInt(Integer::intValue).sum();

        // Only test the case where sum exceeds pool
        Assume.that(totalRaw > poolTotal);
        Assume.that(totalRaw > 0);

        SalaryServiceImpl service = createService(poolTotal);

        try {
            List<Integer> adjusted = invokeAdjustToPool(service, rawCoins, totalRaw);

            assertThat(adjusted).hasSize(rawCoins.size());

            // Verify each member's adjusted share is close to the expected proportional value.
            // Expected: adjusted[i] ≈ raw[i] * poolTotal / totalRaw
            // The implementation uses floor rounding for all but the last member,
            // and the last member gets the remainder. So each member's error is bounded
            // by (memberCount - 1) to account for the last-member remainder adjustment.
            int memberCount = rawCoins.size();
            for (int i = 0; i < memberCount; i++) {
                double expectedShare = (double) rawCoins.get(i) * poolTotal / totalRaw;
                assertThat((double) adjusted.get(i))
                        .as("Member %d (raw=%d) adjusted share should be proportional: " +
                                        "expected ≈ %.2f, got %d",
                                i, rawCoins.get(i), expectedShare, adjusted.get(i))
                        .isCloseTo(expectedShare, org.assertj.core.data.Offset.offset((double) memberCount));
            }

            // Also verify the total adjusted equals poolTotal
            int totalAdjusted = adjusted.stream().mapToInt(Integer::intValue).sum();
            assertThat(totalAdjusted)
                    .as("Total adjusted coins should equal pool total")
                    .isEqualTo(poolTotal);

        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke adjustToPool", e);
        }
    }

    @Property(tries = 100)
    void property7_whenSumDoesNotExceedPool_coinsRemainUnchanged(
            @ForAll("positiveRawCoinsList") List<Integer> rawCoins,
            @ForAll("largeSalaryPoolTotal") int poolTotal) {

        int totalRaw = rawCoins.stream().mapToInt(Integer::intValue).sum();

        // Only test the case where sum does NOT exceed pool
        Assume.that(totalRaw > 0);
        Assume.that(totalRaw <= poolTotal);

        SalaryServiceImpl service = createService(poolTotal);

        try {
            List<Integer> adjusted = invokeAdjustToPool(service, rawCoins, totalRaw);

            assertThat(adjusted)
                    .as("When sum of raw coins (%d) does not exceed pool total (%d), " +
                            "adjusted coins should equal raw coins", totalRaw, poolTotal)
                    .isEqualTo(rawCoins);

        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke adjustToPool", e);
        }
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<List<Integer>> positiveRawCoinsList() {
        return Arbitraries.integers().between(1, 800)
                .list().ofSize(MEMBER_COUNT);
    }

    @Provide
    Arbitrary<Integer> salaryPoolTotal() {
        return Arbitraries.integers().between(500, 2000);
    }

    @Provide
    Arbitrary<Integer> largeSalaryPoolTotal() {
        return Arbitraries.integers().between(4000, 10000);
    }
}
