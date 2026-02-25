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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for points-to-mini-coins conversion correctness.
 *
 * Feature: salary-calculation-rules, Property 6: 积分转迷你币换算正确性
 * For any 总积分值和配置的换算比例，计算得到的迷你币应等于总积分乘以换算比例。
 *
 * **Validates: Requirements 4.1, 5.1, 5.2**
 */
class PointsToMiniCoinsPropertyTest {

    private static final List<CheckinTier> DEFAULT_TIERS = List.of(
            CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build(),
            CheckinTier.builder().minCount(20).maxCount(29).points(-10).label("需改进").build(),
            CheckinTier.builder().minCount(30).maxCount(39).points(0).label("合格").build(),
            CheckinTier.builder().minCount(40).maxCount(49).points(30).label("良好").build(),
            CheckinTier.builder().minCount(50).maxCount(999).points(50).label("优秀").build()
    );

    private SalaryServiceImpl createService(int pointsToCoinsRatio) {
        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getCheckinTiers()).thenReturn(DEFAULT_TIERS);
        when(configService.getPointsToCoinsRatio()).thenReturn(pointsToCoinsRatio);

        return new SalaryServiceImpl(
                mock(SalaryRecordRepository.class),
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                configService
        );
    }

    // ========================================================================
    // Property 6: 积分转迷你币换算正确性
    // **Validates: Requirements 4.1, 5.1, 5.2**
    // ========================================================================

    @Property(tries = 100)
    void property6_miniCoinsEqualsTotalPointsTimesRatio(
            @ForAll("validCommunityActivity") int communityActivity,
            @ForAll("validCheckinCount") int checkinCount,
            @ForAll("validViolationCount") int violationCount,
            @ForAll("validTaskCompletion") int taskCompletion,
            @ForAll("validAnnouncementCount") int announcementCount,
            @ForAll("validEventHosting") int eventHosting,
            @ForAll("validBirthdayBonus") int birthdayBonus,
            @ForAll("validMonthlyExcellent") int monthlyExcellent,
            @ForAll("validPointsToCoinsRatio") int ratio) {

        SalaryServiceImpl service = createService(ratio);

        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(communityActivity)
                .checkinCount(checkinCount)
                .violationHandlingCount(violationCount)
                .taskCompletionPoints(taskCompletion)
                .announcementCount(announcementCount)
                .eventHostingPoints(eventHosting)
                .birthdayBonusPoints(birthdayBonus)
                .monthlyExcellentPoints(monthlyExcellent)
                .build();

        SalaryCalculationResult result = service.calculateMemberPoints(input);

        int expectedMiniCoins = result.getTotalPoints() * ratio;

        assertThat(result.getMiniCoins())
                .as("miniCoins should equal totalPoints(%d) × ratio(%d) = %d",
                        result.getTotalPoints(), ratio, expectedMiniCoins)
                .isEqualTo(expectedMiniCoins);
    }

    // ========================================================================
    // Providers - constrained to valid input ranges
    // ========================================================================

    @Provide
    Arbitrary<Integer> validCommunityActivity() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> validCheckinCount() {
        return Arbitraries.integers().between(0, 999);
    }

    @Provide
    Arbitrary<Integer> validViolationCount() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> validTaskCompletion() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> validAnnouncementCount() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> validEventHosting() {
        return Arbitraries.integers().between(0, 250);
    }

    @Provide
    Arbitrary<Integer> validBirthdayBonus() {
        return Arbitraries.integers().between(0, 25);
    }

    @Provide
    Arbitrary<Integer> validMonthlyExcellent() {
        return Arbitraries.integers().between(0, 30);
    }

    @Provide
    Arbitrary<Integer> validPointsToCoinsRatio() {
        return Arbitraries.integers().between(1, 10);
    }
}
