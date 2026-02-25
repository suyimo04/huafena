package com.pollen.management.service;

import com.pollen.management.entity.WeeklyReport;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private RoleChangeHistoryRepository roleChangeHistoryRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private PointsRecordRepository pointsRecordRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static final LocalDate WEEK_START = LocalDate.of(2024, 1, 8); // Monday
    private static final LocalDate WEEK_END = LocalDate.of(2024, 1, 14);  // Sunday

    @Test
    void generateWeeklyReport_shouldAggregateDataAndSave() {
        LocalDateTime rangeStart = WEEK_START.atStartOfDay();
        LocalDateTime rangeEnd = WEEK_END.atTime(LocalTime.MAX);

        when(weeklyReportRepository.findByWeekStartAndWeekEnd(WEEK_START, WEEK_END))
                .thenReturn(Optional.empty());
        when(applicationRepository.countByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn(10L);
        when(interviewRepository.countByCompletedAtBetween(rangeStart, rangeEnd)).thenReturn(5L);
        when(roleChangeHistoryRepository.countByNewRoleAndChangedAtBetween(
                eq(Role.MEMBER), eq(rangeStart), eq(rangeEnd))).thenReturn(2L);
        when(activityRepository.countByActivityTimeBetween(rangeStart, rangeEnd)).thenReturn(3L);
        when(pointsRecordRepository.sumPositiveAmountByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn(150);
        when(weeklyReportRepository.save(any(WeeklyReport.class))).thenAnswer(inv -> {
            WeeklyReport r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        WeeklyReport report = reportService.generateWeeklyReport(WEEK_START, WEEK_END);

        assertThat(report.getWeekStart()).isEqualTo(WEEK_START);
        assertThat(report.getWeekEnd()).isEqualTo(WEEK_END);
        assertThat(report.getNewApplications()).isEqualTo(10);
        assertThat(report.getInterviewsCompleted()).isEqualTo(5);
        assertThat(report.getNewMembers()).isEqualTo(2);
        assertThat(report.getActivitiesHeld()).isEqualTo(3);
        assertThat(report.getTotalPointsIssued()).isEqualTo(150);
        verify(weeklyReportRepository).save(any(WeeklyReport.class));
    }

    @Test
    void generateWeeklyReport_shouldReturnExistingIfAlreadyGenerated() {
        WeeklyReport existing = WeeklyReport.builder()
                .id(1L)
                .weekStart(WEEK_START)
                .weekEnd(WEEK_END)
                .newApplications(5)
                .build();
        when(weeklyReportRepository.findByWeekStartAndWeekEnd(WEEK_START, WEEK_END))
                .thenReturn(Optional.of(existing));

        WeeklyReport report = reportService.generateWeeklyReport(WEEK_START, WEEK_END);

        assertThat(report.getId()).isEqualTo(1L);
        assertThat(report.getNewApplications()).isEqualTo(5);
        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    void generateWeeklyReport_withNoData_shouldReturnZeros() {
        LocalDateTime rangeStart = WEEK_START.atStartOfDay();
        LocalDateTime rangeEnd = WEEK_END.atTime(LocalTime.MAX);

        when(weeklyReportRepository.findByWeekStartAndWeekEnd(WEEK_START, WEEK_END))
                .thenReturn(Optional.empty());
        when(applicationRepository.countByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn(0L);
        when(interviewRepository.countByCompletedAtBetween(rangeStart, rangeEnd)).thenReturn(0L);
        when(roleChangeHistoryRepository.countByNewRoleAndChangedAtBetween(
                eq(Role.MEMBER), eq(rangeStart), eq(rangeEnd))).thenReturn(0L);
        when(activityRepository.countByActivityTimeBetween(rangeStart, rangeEnd)).thenReturn(0L);
        when(pointsRecordRepository.sumPositiveAmountByCreatedAtBetween(rangeStart, rangeEnd)).thenReturn(0);
        when(weeklyReportRepository.save(any(WeeklyReport.class))).thenAnswer(inv -> inv.getArgument(0));

        WeeklyReport report = reportService.generateWeeklyReport(WEEK_START, WEEK_END);

        assertThat(report.getNewApplications()).isZero();
        assertThat(report.getInterviewsCompleted()).isZero();
        assertThat(report.getNewMembers()).isZero();
        assertThat(report.getActivitiesHeld()).isZero();
        assertThat(report.getTotalPointsIssued()).isZero();
    }

    @Test
    void listWeeklyReports_shouldReturnOrderedByWeekStartDesc() {
        WeeklyReport r1 = WeeklyReport.builder().id(1L).weekStart(LocalDate.of(2024, 1, 1)).build();
        WeeklyReport r2 = WeeklyReport.builder().id(2L).weekStart(LocalDate.of(2024, 1, 8)).build();
        when(weeklyReportRepository.findAllByOrderByWeekStartDesc()).thenReturn(List.of(r2, r1));

        List<WeeklyReport> reports = reportService.listWeeklyReports();

        assertThat(reports).hasSize(2);
        assertThat(reports.get(0).getWeekStart()).isAfter(reports.get(1).getWeekStart());
    }

    @Test
    void listWeeklyReports_shouldReturnEmptyListWhenNoReports() {
        when(weeklyReportRepository.findAllByOrderByWeekStartDesc()).thenReturn(List.of());

        List<WeeklyReport> reports = reportService.listWeeklyReports();

        assertThat(reports).isEmpty();
    }

    @Test
    void getWeeklyReport_shouldReturnReportById() {
        WeeklyReport report = WeeklyReport.builder()
                .id(1L)
                .weekStart(WEEK_START)
                .weekEnd(WEEK_END)
                .newApplications(10)
                .build();
        when(weeklyReportRepository.findById(1L)).thenReturn(Optional.of(report));

        WeeklyReport result = reportService.getWeeklyReport(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNewApplications()).isEqualTo(10);
    }

    @Test
    void getWeeklyReport_shouldThrowWhenNotFound() {
        when(weeklyReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getWeeklyReport(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("周报不存在");
    }
}
