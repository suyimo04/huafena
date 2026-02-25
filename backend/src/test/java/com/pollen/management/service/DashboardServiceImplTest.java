package com.pollen.management.service;

import com.pollen.management.dto.MemberSalaryRank;
import com.pollen.management.dto.OperationsDataDTO;
import com.pollen.management.dto.RecruitmentStatsDTO;
import com.pollen.management.dto.SalaryStatsDTO;
import com.pollen.management.entity.InterviewReport;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private PointsRecordRepository pointsRecordRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private InterviewReportRepository interviewReportRepository;
    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // --- getRecruitmentStats ---

    @Test
    void getRecruitmentStats_shouldReturnAllStageCountsAndRates() {
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(5L);
        when(applicationRepository.countByStatus(ApplicationStatus.INITIAL_REVIEW_PASSED)).thenReturn(3L);
        when(applicationRepository.countByStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS)).thenReturn(2L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_REVIEW)).thenReturn(1L);
        when(applicationRepository.countByStatus(ApplicationStatus.INTERN_OFFERED)).thenReturn(4L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(6L);

        // 3 reports with review results: 2 APPROVED, 1 REJECTED
        // 1 report with manualApproved=true, 1 with manualApproved=false, 1 with null
        var report1 = InterviewReport.builder().reviewResult("APPROVED").manualApproved(true).build();
        var report2 = InterviewReport.builder().reviewResult("REJECTED").manualApproved(false).build();
        var report3 = InterviewReport.builder().reviewResult("APPROVED").manualApproved(null).build();
        when(interviewReportRepository.findAll()).thenReturn(List.of(report1, report2, report3));

        RecruitmentStatsDTO stats = dashboardService.getRecruitmentStats();

        assertThat(stats.getStageCount()).containsEntry("PENDING_INITIAL_REVIEW", 5L);
        assertThat(stats.getStageCount()).containsEntry("INITIAL_REVIEW_PASSED", 3L);
        assertThat(stats.getStageCount()).containsEntry("AI_INTERVIEW_IN_PROGRESS", 2L);
        assertThat(stats.getStageCount()).containsEntry("PENDING_REVIEW", 1L);
        assertThat(stats.getStageCount()).containsEntry("INTERN_OFFERED", 4L);
        assertThat(stats.getStageCount()).containsEntry("REJECTED", 6L);

        // AI pass rate: 2 approved / 3 reviewed = 0.6667
        assertThat(stats.getAiInterviewPassRate()).isCloseTo(2.0 / 3.0, within(0.001));
        // Manual review pass rate: 1 true / 2 non-null = 0.5
        assertThat(stats.getManualReviewPassRate()).isCloseTo(0.5, within(0.001));
    }

    @Test
    void getRecruitmentStats_withNoReports_shouldReturnZeroRates() {
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.INITIAL_REVIEW_PASSED)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_REVIEW)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.INTERN_OFFERED)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(0L);
        when(interviewReportRepository.findAll()).thenReturn(Collections.emptyList());

        RecruitmentStatsDTO stats = dashboardService.getRecruitmentStats();

        assertThat(stats.getAiInterviewPassRate()).isEqualTo(0.0);
        assertThat(stats.getManualReviewPassRate()).isEqualTo(0.0);
    }

    @Test
    void getRecruitmentStats_withBlankReviewResult_shouldNotCountAsReviewed() {
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.INITIAL_REVIEW_PASSED)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_REVIEW)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.INTERN_OFFERED)).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(0L);
        var reportBlank = InterviewReport.builder().reviewResult("  ").manualApproved(null).build();
        var reportNull = InterviewReport.builder().reviewResult(null).manualApproved(null).build();
        when(interviewReportRepository.findAll()).thenReturn(List.of(reportBlank, reportNull));

        RecruitmentStatsDTO stats = dashboardService.getRecruitmentStats();

        assertThat(stats.getAiInterviewPassRate()).isEqualTo(0.0);
        assertThat(stats.getManualReviewPassRate()).isEqualTo(0.0);
    }

    // --- getSalaryStats ---

    @Test
    void getSalaryStats_shouldReturnPoolUsageAndRanking() {
        var user1 = User.builder().id(1L).username("alice").role(Role.MEMBER).build();
        var user2 = User.builder().id(2L).username("bob").role(Role.VICE_LEADER).build();
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        var salary1 = SalaryRecord.builder().userId(1L).totalPoints(200).miniCoins(400).archived(false).build();
        var salary2 = SalaryRecord.builder().userId(2L).totalPoints(150).miniCoins(300).archived(false).build();
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(List.of(salary1, salary2));

        SalaryStatsDTO stats = dashboardService.getSalaryStats();

        assertThat(stats.getTotalPool()).isEqualTo(2000);
        assertThat(stats.getAllocated()).isEqualTo(700);
        assertThat(stats.getUsageRate()).isCloseTo(700.0 / 2000.0, within(0.001));
        assertThat(stats.getRanking()).hasSize(2);
        // Sorted by miniCoins descending
        assertThat(stats.getRanking().get(0).getMiniCoins()).isEqualTo(400);
        assertThat(stats.getRanking().get(0).getUsername()).isEqualTo("alice");
        assertThat(stats.getRanking().get(1).getMiniCoins()).isEqualTo(300);
        assertThat(stats.getRanking().get(1).getUsername()).isEqualTo("bob");
    }

    @Test
    void getSalaryStats_withNoRecords_shouldReturnZeroAllocated() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(Collections.emptyList());

        SalaryStatsDTO stats = dashboardService.getSalaryStats();

        assertThat(stats.getTotalPool()).isEqualTo(2000);
        assertThat(stats.getAllocated()).isEqualTo(0);
        assertThat(stats.getUsageRate()).isEqualTo(0.0);
        assertThat(stats.getRanking()).isEmpty();
    }

    @Test
    void getSalaryStats_withUnknownUser_shouldShowUnknownUsername() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        var salary = SalaryRecord.builder().userId(99L).totalPoints(100).miniCoins(200).archived(false).build();
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(List.of(salary));

        SalaryStatsDTO stats = dashboardService.getSalaryStats();

        assertThat(stats.getRanking()).hasSize(1);
        assertThat(stats.getRanking().get(0).getUsername()).isEqualTo("unknown");
    }

    // --- getOperationsData ---

    @Test
    void getOperationsData_shouldReturnGrowthTrendAndProcessingStats() {
        // Users created in different months
        var user1 = User.builder().id(1L).username("u1").role(Role.MEMBER).build();
        user1.setCreatedAt(LocalDateTime.now().minusMonths(1).withDayOfMonth(15));
        var user2 = User.builder().id(2L).username("u2").role(Role.MEMBER).build();
        user2.setCreatedAt(LocalDateTime.now().minusMonths(1).withDayOfMonth(20));
        var user3 = User.builder().id(3L).username("u3").role(Role.INTERN).build();
        user3.setCreatedAt(LocalDateTime.now().withDayOfMonth(5));

        when(userRepository.findByCreatedAtAfter(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(user1, user2, user3));

        // Applications: 10 total, 3 pending
        when(applicationRepository.count()).thenReturn(10L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(3L);

        OperationsDataDTO data = dashboardService.getOperationsData();

        // Growth trend should have 12 months
        assertThat(data.getUserGrowthTrend()).hasSize(12);

        // Current month should have 1 user
        var currentMonth = data.getUserGrowthTrend().get(11);
        assertThat(currentMonth.getMonth()).isEqualTo(java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        assertThat(currentMonth.getCount()).isEqualTo(1);

        // Previous month should have 2 users
        var prevMonth = data.getUserGrowthTrend().get(10);
        assertThat(prevMonth.getMonth()).isEqualTo(java.time.YearMonth.now().minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        assertThat(prevMonth.getCount()).isEqualTo(2);

        // Issue processing stats
        assertThat(data.getIssueProcessingStats()).containsEntry("totalApplications", 10L);
        assertThat(data.getIssueProcessingStats()).containsEntry("processedApplications", 7L);
        assertThat(data.getIssueProcessingStats()).containsEntry("pendingApplications", 3L);
        assertThat((double) data.getIssueProcessingStats().get("processingRate")).isCloseTo(0.7, within(0.001));
    }

    @Test
    void getOperationsData_withNoUsers_shouldReturnZeroCountsForAllMonths() {
        when(userRepository.findByCreatedAtAfter(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(applicationRepository.count()).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(0L);

        OperationsDataDTO data = dashboardService.getOperationsData();

        assertThat(data.getUserGrowthTrend()).hasSize(12);
        data.getUserGrowthTrend().forEach(m -> assertThat(m.getCount()).isZero());

        assertThat(data.getIssueProcessingStats()).containsEntry("totalApplications", 0L);
        assertThat(data.getIssueProcessingStats()).containsEntry("processedApplications", 0L);
        assertThat((double) data.getIssueProcessingStats().get("processingRate")).isEqualTo(0.0);
    }

    @Test
    void getOperationsData_growthTrendShouldBeChronologicallyOrdered() {
        when(userRepository.findByCreatedAtAfter(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(applicationRepository.count()).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(0L);

        OperationsDataDTO data = dashboardService.getOperationsData();

        // Verify months are in ascending chronological order
        for (int i = 1; i < data.getUserGrowthTrend().size(); i++) {
            String prev = data.getUserGrowthTrend().get(i - 1).getMonth();
            String curr = data.getUserGrowthTrend().get(i).getMonth();
            assertThat(prev.compareTo(curr)).isLessThan(0);
        }
    }

    @Test
    void getOperationsData_withNullCreatedAt_shouldSkipUser() {
        var userWithNull = User.builder().id(1L).username("u1").role(Role.MEMBER).build();
        // createdAt is null (not set via @PrePersist since we're building manually)

        when(userRepository.findByCreatedAtAfter(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(userWithNull));
        when(applicationRepository.count()).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW)).thenReturn(0L);

        OperationsDataDTO data = dashboardService.getOperationsData();

        // All months should have 0 count since the user has null createdAt
        assertThat(data.getUserGrowthTrend()).hasSize(12);
        data.getUserGrowthTrend().forEach(m -> assertThat(m.getCount()).isZero());
    }
}
