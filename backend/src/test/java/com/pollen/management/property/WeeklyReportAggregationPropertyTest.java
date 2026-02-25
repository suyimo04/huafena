package com.pollen.management.property;

import com.pollen.management.entity.WeeklyReport;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import com.pollen.management.service.ReportServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for weekly report data aggregation correctness.
 *
 * Property 41: 周报数据聚合正确性
 * For any valid week range, the generated report's fields match the repository query results.
 * All numeric fields are non-negative. weekStart is always before weekEnd.
 * Idempotent: generating the same week twice returns the same report.
 *
 * **Validates: Requirements 16.1**
 */
class WeeklyReportAggregationPropertyTest {

    // ========================================================================
    // Property 41: 周报数据聚合正确性 — fields match repository query results
    // **Validates: Requirements 16.1**
    // ========================================================================

    @Property(tries = 200)
    void property41_reportFieldsMatchRepositoryQueryResults(
            @ForAll("weekRanges") LocalDate[] weekRange,
            @ForAll @IntRange(min = 0, max = 500) int newApps,
            @ForAll @IntRange(min = 0, max = 500) int interviews,
            @ForAll @IntRange(min = 0, max = 100) int newMembers,
            @ForAll @IntRange(min = 0, max = 200) int activities,
            @ForAll @IntRange(min = 0, max = 10000) int points) {

        LocalDate weekStart = weekRange[0];
        LocalDate weekEnd = weekRange[1];
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = weekEnd.atTime(LocalTime.MAX);

        ReportServiceImpl service = buildServiceWithMocks(
                weekStart, weekEnd, rangeStart, rangeEnd,
                newApps, interviews, newMembers, activities, points,
                /* existingReport= */ false);

        WeeklyReport report = service.generateWeeklyReport(weekStart, weekEnd);

        assertThat(report.getNewApplications())
                .as("newApplications must match repository count")
                .isEqualTo(newApps);
        assertThat(report.getInterviewsCompleted())
                .as("interviewsCompleted must match repository count")
                .isEqualTo(interviews);
        assertThat(report.getNewMembers())
                .as("newMembers must match repository count")
                .isEqualTo(newMembers);
        assertThat(report.getActivitiesHeld())
                .as("activitiesHeld must match repository count")
                .isEqualTo(activities);
        assertThat(report.getTotalPointsIssued())
                .as("totalPointsIssued must match repository sum")
                .isEqualTo(points);
    }

    // ========================================================================
    // Property 41: All numeric fields are non-negative
    // **Validates: Requirements 16.1**
    // ========================================================================

    @Property(tries = 200)
    void property41_allNumericFieldsAreNonNegative(
            @ForAll("weekRanges") LocalDate[] weekRange,
            @ForAll @IntRange(min = 0, max = 500) int newApps,
            @ForAll @IntRange(min = 0, max = 500) int interviews,
            @ForAll @IntRange(min = 0, max = 100) int newMembers,
            @ForAll @IntRange(min = 0, max = 200) int activities,
            @ForAll @IntRange(min = 0, max = 10000) int points) {

        LocalDate weekStart = weekRange[0];
        LocalDate weekEnd = weekRange[1];
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = weekEnd.atTime(LocalTime.MAX);

        ReportServiceImpl service = buildServiceWithMocks(
                weekStart, weekEnd, rangeStart, rangeEnd,
                newApps, interviews, newMembers, activities, points,
                false);

        WeeklyReport report = service.generateWeeklyReport(weekStart, weekEnd);

        assertThat(report.getNewApplications()).isGreaterThanOrEqualTo(0);
        assertThat(report.getInterviewsCompleted()).isGreaterThanOrEqualTo(0);
        assertThat(report.getNewMembers()).isGreaterThanOrEqualTo(0);
        assertThat(report.getActivitiesHeld()).isGreaterThanOrEqualTo(0);
        assertThat(report.getTotalPointsIssued()).isGreaterThanOrEqualTo(0);
    }

    // ========================================================================
    // Property 41: weekStart is always before weekEnd
    // **Validates: Requirements 16.1**
    // ========================================================================

    @Property(tries = 200)
    void property41_weekStartIsBeforeWeekEnd(
            @ForAll("weekRanges") LocalDate[] weekRange) {

        LocalDate weekStart = weekRange[0];
        LocalDate weekEnd = weekRange[1];

        ReportServiceImpl service = buildServiceWithMocks(
                weekStart, weekEnd,
                weekStart.atStartOfDay(), weekEnd.atTime(LocalTime.MAX),
                0, 0, 0, 0, 0, false);

        WeeklyReport report = service.generateWeeklyReport(weekStart, weekEnd);

        assertThat(report.getWeekStart())
                .as("weekStart must be before weekEnd")
                .isBefore(report.getWeekEnd());
    }

    // ========================================================================
    // Property 41: Idempotent — generating the same week twice returns the same report
    // **Validates: Requirements 16.1**
    // ========================================================================

    @Property(tries = 200)
    void property41_idempotentGenerationReturnsSameReport(
            @ForAll("weekRanges") LocalDate[] weekRange,
            @ForAll @IntRange(min = 0, max = 500) int newApps,
            @ForAll @IntRange(min = 0, max = 500) int interviews,
            @ForAll @IntRange(min = 0, max = 100) int newMembers,
            @ForAll @IntRange(min = 0, max = 200) int activities,
            @ForAll @IntRange(min = 0, max = 10000) int points) {

        LocalDate weekStart = weekRange[0];
        LocalDate weekEnd = weekRange[1];
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = weekEnd.atTime(LocalTime.MAX);

        // First call: no existing report, creates new one
        WeeklyReportRepository weeklyReportRepo = mock(WeeklyReportRepository.class);
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        InterviewRepository interviewRepo = mock(InterviewRepository.class);
        RoleChangeHistoryRepository roleRepo = mock(RoleChangeHistoryRepository.class);
        ActivityRepository activityRepo = mock(ActivityRepository.class);
        PointsRecordRepository pointsRepo = mock(PointsRecordRepository.class);

        WeeklyReport savedReport = WeeklyReport.builder()
                .id(1L)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .newApplications(newApps)
                .interviewsCompleted(interviews)
                .newMembers(newMembers)
                .activitiesHeld(activities)
                .totalPointsIssued(points)
                .build();

        // First call returns empty (triggers creation), second call returns the saved report
        when(weeklyReportRepo.findByWeekStartAndWeekEnd(weekStart, weekEnd))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedReport));

        when(appRepo.countByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn((long) newApps);
        when(interviewRepo.countByCompletedAtBetween(rangeStart, rangeEnd)).thenReturn((long) interviews);
        when(roleRepo.countByNewRoleAndChangedAtBetween(eq(Role.MEMBER), eq(rangeStart), eq(rangeEnd)))
                .thenReturn((long) newMembers);
        when(activityRepo.countByActivityTimeBetween(rangeStart, rangeEnd)).thenReturn((long) activities);
        when(pointsRepo.sumPositiveAmountByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn(points);
        when(weeklyReportRepo.save(any(WeeklyReport.class))).thenReturn(savedReport);

        ReportServiceImpl service = new ReportServiceImpl(
                weeklyReportRepo, appRepo, interviewRepo, roleRepo, activityRepo, pointsRepo);

        WeeklyReport first = service.generateWeeklyReport(weekStart, weekEnd);
        WeeklyReport second = service.generateWeeklyReport(weekStart, weekEnd);

        assertThat(second.getNewApplications()).isEqualTo(first.getNewApplications());
        assertThat(second.getInterviewsCompleted()).isEqualTo(first.getInterviewsCompleted());
        assertThat(second.getNewMembers()).isEqualTo(first.getNewMembers());
        assertThat(second.getActivitiesHeld()).isEqualTo(first.getActivitiesHeld());
        assertThat(second.getTotalPointsIssued()).isEqualTo(first.getTotalPointsIssued());
        assertThat(second.getWeekStart()).isEqualTo(first.getWeekStart());
        assertThat(second.getWeekEnd()).isEqualTo(first.getWeekEnd());
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<LocalDate[]> weekRanges() {
        return Arbitraries.integers().between(2020, 2030).flatMap(year ->
                Arbitraries.integers().between(1, 50).map(weekNum -> {
                    // Generate a Monday-Sunday week range
                    LocalDate monday = LocalDate.ofYearDay(year, 1)
                            .with(java.time.temporal.TemporalAdjusters.firstInMonth(java.time.DayOfWeek.MONDAY))
                            .plusWeeks(weekNum - 1);
                    LocalDate sunday = monday.plusDays(6);
                    return new LocalDate[]{monday, sunday};
                })
        );
    }

    // ========== Helpers ==========

    private ReportServiceImpl buildServiceWithMocks(
            LocalDate weekStart, LocalDate weekEnd,
            LocalDateTime rangeStart, LocalDateTime rangeEnd,
            int newApps, int interviews, int newMembers, int activities, int points,
            boolean existingReport) {

        WeeklyReportRepository weeklyReportRepo = mock(WeeklyReportRepository.class);
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        InterviewRepository interviewRepo = mock(InterviewRepository.class);
        RoleChangeHistoryRepository roleRepo = mock(RoleChangeHistoryRepository.class);
        ActivityRepository activityRepo = mock(ActivityRepository.class);
        PointsRecordRepository pointsRepo = mock(PointsRecordRepository.class);

        if (existingReport) {
            WeeklyReport existing = WeeklyReport.builder()
                    .id(1L).weekStart(weekStart).weekEnd(weekEnd)
                    .newApplications(newApps).interviewsCompleted(interviews)
                    .newMembers(newMembers).activitiesHeld(activities)
                    .totalPointsIssued(points).build();
            when(weeklyReportRepo.findByWeekStartAndWeekEnd(weekStart, weekEnd))
                    .thenReturn(Optional.of(existing));
        } else {
            when(weeklyReportRepo.findByWeekStartAndWeekEnd(weekStart, weekEnd))
                    .thenReturn(Optional.empty());
            when(appRepo.countByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn((long) newApps);
            when(interviewRepo.countByCompletedAtBetween(rangeStart, rangeEnd)).thenReturn((long) interviews);
            when(roleRepo.countByNewRoleAndChangedAtBetween(eq(Role.MEMBER), eq(rangeStart), eq(rangeEnd)))
                    .thenReturn((long) newMembers);
            when(activityRepo.countByActivityTimeBetween(rangeStart, rangeEnd)).thenReturn((long) activities);
            when(pointsRepo.sumPositiveAmountByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn(points);
            when(weeklyReportRepo.save(any(WeeklyReport.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        return new ReportServiceImpl(
                weeklyReportRepo, appRepo, interviewRepo, roleRepo, activityRepo, pointsRepo);
    }
}
