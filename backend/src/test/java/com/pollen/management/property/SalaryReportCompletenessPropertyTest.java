package com.pollen.management.property;

import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.dto.SalaryReportDTO;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.PointsService;
import com.pollen.management.service.SalaryConfigService;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for salary report completeness.
 *
 * Feature: salary-calculation-rules, Property 9: 薪酬报表完整性
 * For any 一组薪资记录，生成的薪酬报表应包含每位成员的所有积分维度明细字段，
 * 且报表中的已分配总额应等于所有成员迷你币之和，已分配总额加剩余额度应等于薪酬池总额。
 *
 * **Validates: Requirements 7.1, 7.2, 7.3**
 */
class SalaryReportCompletenessPropertyTest {

    // ========================================================================
    // Property 9: 薪酬报表完整性
    // **Validates: Requirements 7.1, 7.2, 7.3**
    // ========================================================================

    @Property(tries = 100)
    void property9_reportContainsAllMembersWithAllDimensionDetails(
            @ForAll("salaryRecordList") List<SalaryRecord> records,
            @ForAll("salaryPoolTotal") int poolTotal) {

        // Setup mocks
        SalaryRecordRepository salaryRecordRepository = mock(SalaryRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SalaryConfigService salaryConfigService = mock(SalaryConfigService.class);

        when(salaryRecordRepository.findByPeriod("2025-07")).thenReturn(records);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(poolTotal);

        // Mock user lookup for each record
        for (SalaryRecord record : records) {
            User user = User.builder()
                    .id(record.getUserId())
                    .username("user_" + record.getUserId())
                    .role(Role.MEMBER)
                    .password("pass")
                    .build();
            when(userRepository.findById(record.getUserId())).thenReturn(Optional.of(user));
        }

        SalaryServiceImpl service = new SalaryServiceImpl(
                salaryRecordRepository,
                userRepository,
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                salaryConfigService
        );

        SalaryReportDTO report = service.generateSalaryReport("2025-07");

        // Verify: report contains all members
        assertThat(report.getDetails()).hasSize(records.size());

        // Verify: each member has all dimension detail fields matching the record
        List<Long> reportUserIds = report.getDetails().stream()
                .map(SalaryReportDTO.MemberSalaryDetail::getUserId)
                .collect(Collectors.toList());
        List<Long> recordUserIds = records.stream()
                .map(SalaryRecord::getUserId)
                .collect(Collectors.toList());
        assertThat(reportUserIds).containsExactlyElementsOf(recordUserIds);

        for (int i = 0; i < records.size(); i++) {
            SalaryRecord record = records.get(i);
            SalaryReportDTO.MemberSalaryDetail detail = report.getDetails().get(i);

            // Requirement 7.1: 各积分维度明细
            assertThat(detail.getCommunityActivityPoints()).isEqualTo(record.getCommunityActivityPoints());
            assertThat(detail.getCheckinCount()).isEqualTo(record.getCheckinCount());
            assertThat(detail.getCheckinPoints()).isEqualTo(record.getCheckinPoints());
            assertThat(detail.getViolationHandlingCount()).isEqualTo(record.getViolationHandlingCount());
            assertThat(detail.getViolationHandlingPoints()).isEqualTo(record.getViolationHandlingPoints());
            assertThat(detail.getTaskCompletionPoints()).isEqualTo(record.getTaskCompletionPoints());
            assertThat(detail.getAnnouncementCount()).isEqualTo(record.getAnnouncementCount());
            assertThat(detail.getAnnouncementPoints()).isEqualTo(record.getAnnouncementPoints());
            assertThat(detail.getEventHostingPoints()).isEqualTo(record.getEventHostingPoints());
            assertThat(detail.getBirthdayBonusPoints()).isEqualTo(record.getBirthdayBonusPoints());
            assertThat(detail.getMonthlyExcellentPoints()).isEqualTo(record.getMonthlyExcellentPoints());

            // Requirement 7.2: 基础积分、奖励积分、总积分和最终迷你币
            assertThat(detail.getBasePoints()).isEqualTo(record.getBasePoints());
            assertThat(detail.getBonusPoints()).isEqualTo(record.getBonusPoints());
            assertThat(detail.getTotalPoints()).isEqualTo(record.getTotalPoints());
            assertThat(detail.getMiniCoins()).isEqualTo(record.getMiniCoins());
        }
    }

    @Property(tries = 100)
    void property9_allocatedTotalEqualsSumOfMemberMiniCoins(
            @ForAll("salaryRecordList") List<SalaryRecord> records,
            @ForAll("salaryPoolTotal") int poolTotal) {

        SalaryRecordRepository salaryRecordRepository = mock(SalaryRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SalaryConfigService salaryConfigService = mock(SalaryConfigService.class);

        when(salaryRecordRepository.findByPeriod("2025-07")).thenReturn(records);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(poolTotal);
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        SalaryServiceImpl service = new SalaryServiceImpl(
                salaryRecordRepository,
                userRepository,
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                salaryConfigService
        );

        SalaryReportDTO report = service.generateSalaryReport("2025-07");

        int expectedAllocatedTotal = records.stream()
                .mapToInt(SalaryRecord::getMiniCoins)
                .sum();

        // Requirement 7.3: allocatedTotal == sum of all members' miniCoins
        assertThat(report.getAllocatedTotal())
                .as("allocatedTotal should equal sum of all members' miniCoins")
                .isEqualTo(expectedAllocatedTotal);
    }

    @Property(tries = 100)
    void property9_allocatedTotalPlusRemainingEqualsSalaryPoolTotal(
            @ForAll("salaryRecordList") List<SalaryRecord> records,
            @ForAll("salaryPoolTotal") int poolTotal) {

        SalaryRecordRepository salaryRecordRepository = mock(SalaryRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SalaryConfigService salaryConfigService = mock(SalaryConfigService.class);

        when(salaryRecordRepository.findByPeriod("2025-07")).thenReturn(records);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(poolTotal);
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        SalaryServiceImpl service = new SalaryServiceImpl(
                salaryRecordRepository,
                userRepository,
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                salaryConfigService
        );

        SalaryReportDTO report = service.generateSalaryReport("2025-07");

        // Requirement 7.3: allocatedTotal + remainingAmount == salaryPoolTotal
        assertThat(report.getAllocatedTotal() + report.getRemainingAmount())
                .as("allocatedTotal(%d) + remainingAmount(%d) should equal salaryPoolTotal(%d)",
                        report.getAllocatedTotal(), report.getRemainingAmount(), poolTotal)
                .isEqualTo(report.getSalaryPoolTotal());
    }

    // ========================================================================
    // Providers - generate random salary records with dimension details
    // ========================================================================

    @Provide
    Arbitrary<List<SalaryRecord>> salaryRecordList() {
        return salaryRecord().list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<SalaryRecord> salaryRecord() {
        // jqwik Combinators.combine supports max 8 params, so we chain two combines
        Arbitrary<int[]> basePart = Combinators.combine(
                Arbitraries.integers().between(0, 100),   // communityActivityPoints
                Arbitraries.integers().between(0, 60),    // checkinCount
                Arbitraries.integers().between(-20, 50),  // checkinPoints
                Arbitraries.integers().between(0, 50),    // violationHandlingCount
                Arbitraries.integers().between(0, 150),   // violationHandlingPoints
                Arbitraries.integers().between(0, 100),   // taskCompletionPoints
                Arbitraries.integers().between(0, 50),    // announcementCount
                Arbitraries.integers().between(0, 250)    // announcementPoints
        ).as((a, b, c, d, e, f, g, h) -> new int[]{a, b, c, d, e, f, g, h});

        Arbitrary<int[]> bonusPart = Combinators.combine(
                Arbitraries.integers().between(0, 250),   // eventHostingPoints
                Arbitraries.integers().between(0, 25),    // birthdayBonusPoints
                Arbitraries.integers().between(0, 30),    // monthlyExcellentPoints
                Arbitraries.integers().between(0, 500),   // basePoints
                Arbitraries.integers().between(0, 300),   // bonusPoints
                Arbitraries.integers().between(0, 800),   // totalPoints
                Arbitraries.integers().between(0, 400)    // miniCoins
        ).as((a, b, c, d, e, f, g) -> new int[]{a, b, c, d, e, f, g});

        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),
                basePart,
                bonusPart
        ).as((userId, base, bonus) ->
                SalaryRecord.builder()
                        .userId(userId)
                        .communityActivityPoints(base[0])
                        .checkinCount(base[1])
                        .checkinPoints(base[2])
                        .violationHandlingCount(base[3])
                        .violationHandlingPoints(base[4])
                        .taskCompletionPoints(base[5])
                        .announcementCount(base[6])
                        .announcementPoints(base[7])
                        .eventHostingPoints(bonus[0])
                        .birthdayBonusPoints(bonus[1])
                        .monthlyExcellentPoints(bonus[2])
                        .basePoints(bonus[3])
                        .bonusPoints(bonus[4])
                        .totalPoints(bonus[5])
                        .miniCoins(bonus[6])
                        .deductions(0)
                        .salaryAmount(BigDecimal.valueOf(bonus[6]))
                        .period("2025-07")
                        .archived(false)
                        .build()
        );
    }

    @Provide
    Arbitrary<Integer> salaryPoolTotal() {
        return Arbitraries.integers().between(500, 5000);
    }
}
