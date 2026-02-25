package com.pollen.management.property;

import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.SalaryDimensionInput;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.PointsService;
import com.pollen.management.service.SalaryConfigService;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for dimension input range validation.
 *
 * Feature: salary-calculation-rules, Property 4: 积分维度输入范围校验
 * For any 积分维度输入值超出该维度允许范围（如社群活跃度超出 0-100），Calculation_Engine 应拒绝该输入。
 *
 * **Validates: Requirements 2.6**
 */
class SalaryDimensionInputRangePropertyTest {

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

    /**
     * Helper: builds a valid input, then applies a single override to push one dimension out of range.
     */
    private SalaryDimensionInput.SalaryDimensionInputBuilder validBase() {
        return SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(50)
                .checkinCount(30)
                .violationHandlingCount(1)
                .taskCompletionPoints(50)
                .announcementCount(1)
                .eventHostingPoints(10)
                .birthdayBonusPoints(10)
                .monthlyExcellentPoints(10);
    }

    // ========================================================================
    // Property 4: 积分维度输入范围校验
    // **Validates: Requirements 2.6**
    // ========================================================================

    @Property(tries = 100)
    void property4_communityActivityOutOfRange_rejected(
            @ForAll("outOfRange_0_100") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .communityActivityPoints(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("社群活跃度");
    }

    @Property(tries = 100)
    void property4_taskCompletionOutOfRange_rejected(
            @ForAll("outOfRange_0_100") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .taskCompletionPoints(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("任务完成");
    }

    @Property(tries = 100)
    void property4_violationHandlingCountNegative_rejected(
            @ForAll("negativeInt") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .violationHandlingCount(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("违规处理");
    }

    @Property(tries = 100)
    void property4_announcementCountNegative_rejected(
            @ForAll("negativeInt") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .announcementCount(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("公告发布");
    }

    @Property(tries = 100)
    void property4_eventHostingOutOfRange_rejected(
            @ForAll("outOfRange_0_250") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .eventHostingPoints(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("活动举办");
    }

    @Property(tries = 100)
    void property4_birthdayBonusOutOfRange_rejected(
            @ForAll("outOfRange_0_25") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .birthdayBonusPoints(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("生日福利");
    }

    @Property(tries = 100)
    void property4_monthlyExcellentOutOfRange_rejected(
            @ForAll("outOfRange_0_30") int invalidValue) {

        SalaryServiceImpl service = createService();
        SalaryDimensionInput input = validBase()
                .monthlyExcellentPoints(invalidValue)
                .build();

        assertThatThrownBy(() -> service.calculateMemberPoints(input))
                .isInstanceOf(IllegalArgumentException.class)
                .message().contains("月度优秀");
    }

    // ========================================================================
    // Providers - generate out-of-range values for each dimension
    // ========================================================================

    @Provide
    Arbitrary<Integer> outOfRange_0_100() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(Integer.MIN_VALUE, -1),
                Arbitraries.integers().between(101, Integer.MAX_VALUE)
        );
    }

    @Provide
    Arbitrary<Integer> negativeInt() {
        return Arbitraries.integers().between(Integer.MIN_VALUE, -1);
    }

    @Provide
    Arbitrary<Integer> outOfRange_0_250() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(Integer.MIN_VALUE, -1),
                Arbitraries.integers().between(251, Integer.MAX_VALUE)
        );
    }

    @Provide
    Arbitrary<Integer> outOfRange_0_25() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(Integer.MIN_VALUE, -1),
                Arbitraries.integers().between(26, Integer.MAX_VALUE)
        );
    }

    @Provide
    Arbitrary<Integer> outOfRange_0_30() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(Integer.MIN_VALUE, -1),
                Arbitraries.integers().between(31, Integer.MAX_VALUE)
        );
    }
}
