package com.pollen.management.property;

import com.pollen.management.dto.RotationThresholds;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.PointsType;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.MemberRotationServiceImpl;
import com.pollen.management.service.SalaryConfigService;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for rotation threshold configuration effectiveness.
 *
 * Feature: salary-calculation-rules, Property 11: 流转阈值配置生效性
 * For any 新的流转阈值配置值，保存后 MemberRotationService 的检测逻辑应使用新配置的阈值而非硬编码值。
 *
 * **Validates: Requirements 6.2**
 */
class RotationThresholdEffectivenessPropertyTest {

    // ========================================================================
    // Property 11: 流转阈值配置生效性
    // For any new rotation threshold config values, MemberRotationService's
    // detection logic should use the configured thresholds, not hardcoded values.
    // **Validates: Requirements 6.2**
    // ========================================================================

    /**
     * Property 11a: isMonthlyPointsStable uses configured promotionPointsThreshold.
     *
     * For any threshold T, an intern with monthly points == T should be eligible for promotion,
     * while an intern with monthly points == T-1 should NOT be eligible.
     */
    @Property(tries = 100)
    void property11_promotionThresholdIsRespected(
            @ForAll("validPromotionThreshold") int promotionThreshold) {

        RotationThresholds thresholds = RotationThresholds.builder()
                .promotionPointsThreshold(promotionThreshold)
                .demotionSalaryThreshold(150)
                .demotionConsecutiveMonths(2)
                .dismissalPointsThreshold(100)
                .dismissalConsecutiveMonths(2)
                .build();

        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getRotationThresholds()).thenReturn(thresholds);

        UserRepository userRepo = mock(UserRepository.class);
        PointsRecordRepository pointsRepo = mock(PointsRecordRepository.class);
        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        RoleChangeHistoryRepository roleChangeRepo = mock(RoleChangeHistoryRepository.class);

        MemberRotationServiceImpl service = new MemberRotationServiceImpl(
                userRepo, pointsRepo, salaryRepo, roleChangeRepo, configService);

        // Create an intern user
        User intern = User.builder().id(1L).username("intern1").password("pass").role(Role.INTERN).build();

        // --- Case 1: points exactly at threshold → should be eligible ---
        when(userRepo.findByRole(Role.INTERN)).thenReturn(List.of(intern));
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Create a single points record with amount == threshold
        PointsRecord atThreshold = PointsRecord.builder()
                .id(1L).userId(1L).amount(promotionThreshold)
                .pointsType(PointsType.TASK_COMPLETION).build();
        when(pointsRepo.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(atThreshold));

        List<User> eligible = service.checkPromotionEligibility();
        assertThat(eligible).as("Intern with points == threshold (%d) should be eligible", promotionThreshold)
                .contains(intern);

        // --- Case 2: points one below threshold → should NOT be eligible ---
        if (promotionThreshold > 0) {
            PointsRecord belowThreshold = PointsRecord.builder()
                    .id(2L).userId(1L).amount(promotionThreshold - 1)
                    .pointsType(PointsType.TASK_COMPLETION).build();
            when(pointsRepo.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                    .thenReturn(List.of(belowThreshold));

            List<User> notEligible = service.checkPromotionEligibility();
            assertThat(notEligible).as("Intern with points == threshold-1 (%d) should NOT be eligible",
                    promotionThreshold - 1).doesNotContain(intern);
        }
    }

    /**
     * Property 11b: hasSalaryBelowThresholdForConsecutiveMonths uses configured
     * demotionSalaryThreshold and demotionConsecutiveMonths.
     *
     * For any threshold T and consecutive months N, a formal member with N archived records
     * all having totalPoints < T should be a demotion candidate, while having at least one
     * record with totalPoints >= T should NOT be a candidate.
     */
    @Property(tries = 100)
    void property11_demotionThresholdAndMonthsAreRespected(
            @ForAll("validDemotionThreshold") int demotionThreshold,
            @ForAll("validConsecutiveMonths") int consecutiveMonths) {

        RotationThresholds thresholds = RotationThresholds.builder()
                .promotionPointsThreshold(100)
                .demotionSalaryThreshold(demotionThreshold)
                .demotionConsecutiveMonths(consecutiveMonths)
                .dismissalPointsThreshold(100)
                .dismissalConsecutiveMonths(2)
                .build();

        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getRotationThresholds()).thenReturn(thresholds);

        UserRepository userRepo = mock(UserRepository.class);
        PointsRecordRepository pointsRepo = mock(PointsRecordRepository.class);
        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        RoleChangeHistoryRepository roleChangeRepo = mock(RoleChangeHistoryRepository.class);

        MemberRotationServiceImpl service = new MemberRotationServiceImpl(
                userRepo, pointsRepo, salaryRepo, roleChangeRepo, configService);

        User member = User.builder().id(2L).username("member1").password("pass").role(Role.MEMBER).build();
        when(userRepo.findByRoleIn(anyList())).thenReturn(List.of(member));

        // --- Case 1: All N archived records below threshold → should be demotion candidate ---
        List<SalaryRecord> allBelowRecords = new ArrayList<>();
        for (int i = 0; i < consecutiveMonths; i++) {
            allBelowRecords.add(SalaryRecord.builder()
                    .id((long) (i + 1)).userId(2L)
                    .totalPoints(demotionThreshold - 1)
                    .archived(true)
                    .build());
        }
        when(salaryRepo.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(2L))
                .thenReturn(allBelowRecords);

        List<User> candidates = service.checkDemotionCandidates();
        assertThat(candidates)
                .as("Member with %d consecutive months below threshold (%d) should be demotion candidate",
                        consecutiveMonths, demotionThreshold)
                .contains(member);

        // --- Case 2: Last record at threshold → should NOT be demotion candidate ---
        List<SalaryRecord> oneAtThreshold = new ArrayList<>();
        // First record is at threshold (not below)
        oneAtThreshold.add(SalaryRecord.builder()
                .id(100L).userId(2L)
                .totalPoints(demotionThreshold)
                .archived(true)
                .build());
        for (int i = 1; i < consecutiveMonths; i++) {
            oneAtThreshold.add(SalaryRecord.builder()
                    .id((long) (100 + i)).userId(2L)
                    .totalPoints(demotionThreshold - 1)
                    .archived(true)
                    .build());
        }
        when(salaryRepo.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(2L))
                .thenReturn(oneAtThreshold);

        List<User> notCandidates = service.checkDemotionCandidates();
        assertThat(notCandidates)
                .as("Member with most recent record at threshold (%d) should NOT be demotion candidate",
                        demotionThreshold)
                .doesNotContain(member);
    }

    /**
     * Property 11c: markForDismissal uses configured dismissalPointsThreshold
     * and dismissalConsecutiveMonths.
     *
     * For any threshold T and consecutive months N, an intern with N months of points < T
     * should be marked for dismissal, while having at least one month with points >= T
     * should NOT be marked.
     */
    @Property(tries = 100)
    void property11_dismissalThresholdAndMonthsAreRespected(
            @ForAll("validDismissalThreshold") int dismissalThreshold,
            @ForAll("validConsecutiveMonths") int consecutiveMonths) {

        RotationThresholds thresholds = RotationThresholds.builder()
                .promotionPointsThreshold(100)
                .demotionSalaryThreshold(150)
                .demotionConsecutiveMonths(2)
                .dismissalPointsThreshold(dismissalThreshold)
                .dismissalConsecutiveMonths(consecutiveMonths)
                .build();

        SalaryConfigService configService = mock(SalaryConfigService.class);
        when(configService.getRotationThresholds()).thenReturn(thresholds);

        UserRepository userRepo = mock(UserRepository.class);
        PointsRecordRepository pointsRepo = mock(PointsRecordRepository.class);
        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        RoleChangeHistoryRepository roleChangeRepo = mock(RoleChangeHistoryRepository.class);

        MemberRotationServiceImpl service = new MemberRotationServiceImpl(
                userRepo, pointsRepo, salaryRepo, roleChangeRepo, configService);

        User intern = User.builder().id(3L).username("intern2").password("pass").role(Role.INTERN).build();
        when(userRepo.findByRole(Role.INTERN)).thenReturn(List.of(intern));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- Case 1: All N previous months below threshold → should be marked ---
        // markForDismissal checks months i=1..N (previous months, not current)
        for (int i = 1; i <= consecutiveMonths; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            LocalDateTime mStart = month.atDay(1).atStartOfDay();
            LocalDateTime mEnd = month.atEndOfMonth().atTime(23, 59, 59);

            PointsRecord belowRecord = PointsRecord.builder()
                    .id((long) i).userId(3L).amount(dismissalThreshold - 1)
                    .pointsType(PointsType.TASK_COMPLETION).build();
            when(pointsRepo.findByUserIdAndCreatedAtBetween(eq(3L), eq(mStart), eq(mEnd)))
                    .thenReturn(List.of(belowRecord));
        }

        List<User> marked = service.markForDismissal();
        assertThat(marked)
                .as("Intern with %d consecutive months below dismissal threshold (%d) should be marked",
                        consecutiveMonths, dismissalThreshold)
                .contains(intern);

        // Reset pendingDismissal for next case
        intern.setPendingDismissal(false);

        // --- Case 2: One month at threshold → should NOT be marked ---
        // Set the first checked month (i=1) to be at threshold
        YearMonth firstMonth = YearMonth.now().minusMonths(1);
        LocalDateTime fStart = firstMonth.atDay(1).atStartOfDay();
        LocalDateTime fEnd = firstMonth.atEndOfMonth().atTime(23, 59, 59);

        PointsRecord atThreshold = PointsRecord.builder()
                .id(100L).userId(3L).amount(dismissalThreshold)
                .pointsType(PointsType.TASK_COMPLETION).build();
        when(pointsRepo.findByUserIdAndCreatedAtBetween(eq(3L), eq(fStart), eq(fEnd)))
                .thenReturn(List.of(atThreshold));

        // Re-create service to get fresh mocks for userRepo.findByRole
        User intern2 = User.builder().id(3L).username("intern2").password("pass").role(Role.INTERN).build();
        when(userRepo.findByRole(Role.INTERN)).thenReturn(List.of(intern2));

        List<User> notMarked = service.markForDismissal();
        assertThat(notMarked)
                .as("Intern with one month at dismissal threshold (%d) should NOT be marked",
                        dismissalThreshold)
                .doesNotContain(intern2);
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<Integer> validPromotionThreshold() {
        return Arbitraries.integers().between(1, 500);
    }

    @Provide
    Arbitrary<Integer> validDemotionThreshold() {
        // Must be > 0 so that threshold-1 is a valid distinct value
        return Arbitraries.integers().between(1, 500);
    }

    @Provide
    Arbitrary<Integer> validDismissalThreshold() {
        // Must be > 0 so that threshold-1 is a valid distinct value
        return Arbitraries.integers().between(1, 500);
    }

    @Provide
    Arbitrary<Integer> validConsecutiveMonths() {
        return Arbitraries.integers().between(1, 6);
    }
}
