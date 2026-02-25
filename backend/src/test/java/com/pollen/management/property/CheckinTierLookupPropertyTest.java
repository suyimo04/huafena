package com.pollen.management.property;

import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.SalaryCalculationResult;
import com.pollen.management.dto.SalaryDimensionInput;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.PointsService;
import com.pollen.management.service.SalaryConfigService;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for checkin tier lookup correctness.
 *
 * Feature: salary-calculation-rules, Property 5: 签到奖惩分级查表正确性
 * For any 非负整数签到次数和合法的签到奖惩分级表，lookupCheckinTier 返回的积分值应与签到次数所落入的分级区间对应的积分值一致。
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 */
class CheckinTierLookupPropertyTest {

    private static final List<CheckinTier> DEFAULT_TIERS = List.of(
            CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build(),
            CheckinTier.builder().minCount(20).maxCount(29).points(-10).label("需改进").build(),
            CheckinTier.builder().minCount(30).maxCount(39).points(0).label("合格").build(),
            CheckinTier.builder().minCount(40).maxCount(49).points(30).label("良好").build(),
            CheckinTier.builder().minCount(50).maxCount(999).points(50).label("优秀").build()
    );

    private SalaryServiceImpl createService(List<CheckinTier> tiers) {
        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getCheckinTiers()).thenReturn(tiers);
        when(configService.getPointsToCoinsRatio()).thenReturn(2);

        return new SalaryServiceImpl(
                mock(SalaryRecordRepository.class),
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                configService
        );
    }

    /**
     * Reference implementation: find the tier whose [minCount, maxCount] contains the count.
     * Negative counts are treated as 0.
     */
    private int expectedCheckinPoints(int count, List<CheckinTier> tiers) {
        if (count < 0) count = 0;
        for (CheckinTier tier : tiers) {
            if (count >= tier.getMinCount() && count <= tier.getMaxCount()) {
                return tier.getPoints();
            }
        }
        return 0;
    }

    /**
     * Build a SalaryDimensionInput with only checkinCount set (all other dimensions zero)
     * so that basePoints == checkinPoints, isolating the lookup behavior.
     */
    private SalaryDimensionInput inputWithCheckinOnly(int checkinCount) {
        return SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(0)
                .checkinCount(checkinCount)
                .violationHandlingCount(0)
                .taskCompletionPoints(0)
                .announcementCount(0)
                .eventHostingPoints(0)
                .birthdayBonusPoints(0)
                .monthlyExcellentPoints(0)
                .build();
    }

    // ========================================================================
    // Property 5: 签到奖惩分级查表正确性 — default tiers
    // **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
    // ========================================================================

    @Property(tries = 200)
    void property5_lookupReturnsCorrectPointsForDefaultTiers(
            @ForAll("nonNegativeCheckinCount") int checkinCount) {

        SalaryServiceImpl service = createService(DEFAULT_TIERS);
        SalaryCalculationResult result = service.calculateMemberPoints(inputWithCheckinOnly(checkinCount));

        int expected = expectedCheckinPoints(checkinCount, DEFAULT_TIERS);

        // With all other dimensions zero, basePoints == checkinPoints
        assertThat(result.getCheckinPoints())
                .as("checkinPoints for count=%d should be %d", checkinCount, expected)
                .isEqualTo(expected);
        assertThat(result.getBasePoints())
                .as("basePoints (only checkin) for count=%d should be %d", checkinCount, expected)
                .isEqualTo(expected);
    }

    // ========================================================================
    // Property 5: negative counts treated as 0 (falls into first tier)
    // **Validates: Requirements 3.1**
    // ========================================================================

    @Property(tries = 100)
    void property5_negativeCountTreatedAsZero(
            @ForAll("negativeCheckinCount") int negativeCount) {

        SalaryServiceImpl service = createService(DEFAULT_TIERS);
        SalaryCalculationResult result = service.calculateMemberPoints(inputWithCheckinOnly(negativeCount));

        int expectedForZero = expectedCheckinPoints(0, DEFAULT_TIERS);

        assertThat(result.getCheckinPoints())
                .as("checkinPoints for negative count=%d should equal tier for 0 (%d)", negativeCount, expectedForZero)
                .isEqualTo(expectedForZero);
    }

    // ========================================================================
    // Property 5: lookup works with randomly generated valid tier tables
    // **Validates: Requirements 3.6**
    // ========================================================================

    @Property(tries = 100)
    void property5_lookupReturnsCorrectPointsForRandomTiers(
            @ForAll("validTierTable") List<CheckinTier> tiers,
            @ForAll("nonNegativeCheckinCount") int checkinCount) {

        SalaryServiceImpl service = createService(tiers);
        SalaryCalculationResult result = service.calculateMemberPoints(inputWithCheckinOnly(checkinCount));

        int expected = expectedCheckinPoints(checkinCount, tiers);

        assertThat(result.getCheckinPoints())
                .as("checkinPoints for count=%d with random tiers should be %d", checkinCount, expected)
                .isEqualTo(expected);
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<Integer> nonNegativeCheckinCount() {
        return Arbitraries.integers().between(0, 1500);
    }

    @Provide
    Arbitrary<Integer> negativeCheckinCount() {
        return Arbitraries.integers().between(-1000, -1);
    }

    /**
     * Generates a valid tier table: contiguous non-overlapping ranges starting from 0,
     * with 2-6 tiers and random points per tier.
     */
    @Provide
    Arbitrary<List<CheckinTier>> validTierTable() {
        return Arbitraries.integers().between(2, 6).flatMap(tierCount ->
                Arbitraries.integers().between(5, 50).list().ofSize(tierCount).flatMap(widths ->
                        Arbitraries.integers().between(-50, 100).list().ofSize(tierCount).map(pointsList -> {
                            List<CheckinTier> tiers = new ArrayList<>();
                            int currentMin = 0;
                            for (int i = 0; i < tierCount; i++) {
                                int width = widths.get(i);
                                int maxCount = (i == tierCount - 1) ? 999 : currentMin + width - 1;
                                tiers.add(CheckinTier.builder()
                                        .minCount(currentMin)
                                        .maxCount(maxCount)
                                        .points(pointsList.get(i))
                                        .label("tier-" + i)
                                        .build());
                                currentMin = maxCount + 1;
                            }
                            return tiers;
                        })
                )
        );
    }
}
