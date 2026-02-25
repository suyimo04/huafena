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
 * Property-based tests for salary dimension summary calculation correctness.
 *
 * Feature: salary-calculation-rules, Property 3: 积分维度汇总计算正确性
 * For any 合法的积分维度输入，Calculation_Engine 计算的 basePoints 应等于社群活跃度 + 签到积分 + 违规处理积分 + 任务完成积分 + 公告积分之和，
 * bonusPoints 应等于活动举办积分 + 生日福利积分 + 月度优秀评议积分之和，totalPoints 应等于 basePoints + bonusPoints。
 *
 * **Validates: Requirements 2.3, 2.4, 2.5**
 */
class SalaryDimensionSummaryPropertyTest {

    private static final List<CheckinTier> DEFAULT_TIERS = List.of(
            CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build(),
            CheckinTier.builder().minCount(20).maxCount(29).points(-10).label("需改进").build(),
            CheckinTier.builder().minCount(30).maxCount(39).points(0).label("合格").build(),
            CheckinTier.builder().minCount(40).maxCount(49).points(30).label("良好").build(),
            CheckinTier.builder().minCount(50).maxCount(999).points(50).label("优秀").build()
    );

    private SalaryServiceImpl createService() {
        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getCheckinTiers()).thenReturn(DEFAULT_TIERS);
        when(configService.getPointsToCoinsRatio()).thenReturn(2);

        return new SalaryServiceImpl(
                mock(SalaryRecordRepository.class),
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                configService
        );
    }

    private int expectedCheckinPoints(int checkinCount) {
        if (checkinCount < 0) checkinCount = 0;
        for (CheckinTier tier : DEFAULT_TIERS) {
            if (checkinCount >= tier.getMinCount() && checkinCount <= tier.getMaxCount()) {
                return tier.getPoints();
            }
        }
        return 0;
    }

    // ========================================================================
    // Property 3: 积分维度汇总计算正确性
    // **Validates: Requirements 2.3, 2.4, 2.5**
    // ========================================================================

    @Property(tries = 100)
    void property3_basePointsEqualsSum(
            @ForAll("validCommunityActivity") int communityActivity,
            @ForAll("validCheckinCount") int checkinCount,
            @ForAll("validViolationCount") int violationCount,
            @ForAll("validTaskCompletion") int taskCompletion,
            @ForAll("validAnnouncementCount") int announcementCount,
            @ForAll("validEventHosting") int eventHosting,
            @ForAll("validBirthdayBonus") int birthdayBonus,
            @ForAll("validMonthlyExcellent") int monthlyExcellent) {

        SalaryServiceImpl service = createService();

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

        int checkinPoints = expectedCheckinPoints(checkinCount);
        int violationHandlingPoints = violationCount * 3;
        int announcementPoints = announcementCount * 5;

        int expectedBase = communityActivity + checkinPoints + violationHandlingPoints
                + taskCompletion + announcementPoints;

        assertThat(result.getBasePoints())
                .as("basePoints should equal communityActivity(%d) + checkinPoints(%d) + violationHandling(%d) + taskCompletion(%d) + announcement(%d)",
                        communityActivity, checkinPoints, violationHandlingPoints, taskCompletion, announcementPoints)
                .isEqualTo(expectedBase);
    }

    @Property(tries = 100)
    void property3_bonusPointsEqualsSum(
            @ForAll("validCommunityActivity") int communityActivity,
            @ForAll("validCheckinCount") int checkinCount,
            @ForAll("validViolationCount") int violationCount,
            @ForAll("validTaskCompletion") int taskCompletion,
            @ForAll("validAnnouncementCount") int announcementCount,
            @ForAll("validEventHosting") int eventHosting,
            @ForAll("validBirthdayBonus") int birthdayBonus,
            @ForAll("validMonthlyExcellent") int monthlyExcellent) {

        SalaryServiceImpl service = createService();

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

        int expectedBonus = eventHosting + birthdayBonus + monthlyExcellent;

        assertThat(result.getBonusPoints())
                .as("bonusPoints should equal eventHosting(%d) + birthdayBonus(%d) + monthlyExcellent(%d)",
                        eventHosting, birthdayBonus, monthlyExcellent)
                .isEqualTo(expectedBonus);
    }

    @Property(tries = 100)
    void property3_totalPointsEqualsBasePointsPlusBonusPoints(
            @ForAll("validCommunityActivity") int communityActivity,
            @ForAll("validCheckinCount") int checkinCount,
            @ForAll("validViolationCount") int violationCount,
            @ForAll("validTaskCompletion") int taskCompletion,
            @ForAll("validAnnouncementCount") int announcementCount,
            @ForAll("validEventHosting") int eventHosting,
            @ForAll("validBirthdayBonus") int birthdayBonus,
            @ForAll("validMonthlyExcellent") int monthlyExcellent) {

        SalaryServiceImpl service = createService();

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

        assertThat(result.getTotalPoints())
                .as("totalPoints should equal basePoints(%d) + bonusPoints(%d)",
                        result.getBasePoints(), result.getBonusPoints())
                .isEqualTo(result.getBasePoints() + result.getBonusPoints());
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
}
