package com.pollen.management.service;

import com.pollen.management.config.RedisConfig;
import com.pollen.management.dto.*;
import com.pollen.management.entity.InterviewReport;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewReportRepository interviewReportRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    private static final int SALARY_POOL_TOTAL = 2000;

    @Override
    @Cacheable(value = RedisConfig.CACHE_DASHBOARD, key = "'overview'")
    public DashboardStatsDTO getDashboardStats() {
        return DashboardStatsDTO.builder()
                .totalMembers(userRepository.count())
                .adminCount(userRepository.countByRole(Role.ADMIN))
                .leaderCount(userRepository.countByRole(Role.LEADER))
                .viceLeaderCount(userRepository.countByRole(Role.VICE_LEADER))
                .memberCount(userRepository.countByRole(Role.MEMBER))
                .internCount(userRepository.countByRole(Role.INTERN))
                .applicantCount(userRepository.countByRole(Role.APPLICANT))
                .totalActivities(activityRepository.count())
                .totalPointsRecords(pointsRecordRepository.count())
                .build();
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_DASHBOARD, key = "'recruitment'")
    public RecruitmentStatsDTO getRecruitmentStats() {
        Map<String, Long> stageCount = new LinkedHashMap<>();
        stageCount.put("PENDING_INITIAL_REVIEW", applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW));
        stageCount.put("INITIAL_REVIEW_PASSED", applicationRepository.countByStatus(ApplicationStatus.INITIAL_REVIEW_PASSED));
        stageCount.put("AI_INTERVIEW_IN_PROGRESS", applicationRepository.countByStatus(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS));
        stageCount.put("PENDING_REVIEW", applicationRepository.countByStatus(ApplicationStatus.PENDING_REVIEW));
        stageCount.put("INTERN_OFFERED", applicationRepository.countByStatus(ApplicationStatus.INTERN_OFFERED));
        stageCount.put("REJECTED", applicationRepository.countByStatus(ApplicationStatus.REJECTED));

        // AI 面试通过率 = 复审通过数 / 完成 AI 面试总数（有 reviewResult 的报告）
        List<InterviewReport> allReports = interviewReportRepository.findAll();
        List<InterviewReport> reviewedReports = allReports.stream()
                .filter(r -> r.getReviewResult() != null && !r.getReviewResult().isBlank())
                .toList();

        long aiInterviewTotal = reviewedReports.size();
        long aiInterviewPassed = reviewedReports.stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getReviewResult()))
                .count();
        double aiPassRate = aiInterviewTotal > 0 ? (double) aiInterviewPassed / aiInterviewTotal : 0.0;

        // 人工复审通过率 = manualApproved=true 的数量 / 所有已复审的数量
        List<InterviewReport> manualReviewed = allReports.stream()
                .filter(r -> r.getManualApproved() != null)
                .toList();
        long manualTotal = manualReviewed.size();
        long manualPassed = manualReviewed.stream()
                .filter(r -> Boolean.TRUE.equals(r.getManualApproved()))
                .count();
        double manualPassRate = manualTotal > 0 ? (double) manualPassed / manualTotal : 0.0;

        return RecruitmentStatsDTO.builder()
                .stageCount(stageCount)
                .aiInterviewPassRate(aiPassRate)
                .manualReviewPassRate(manualPassRate)
                .build();
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_DASHBOARD, key = "'salary'")
    public SalaryStatsDTO getSalaryStats() {
        List<SalaryRecord> activeRecords = salaryRecordRepository.findByArchivedFalse();

        int allocated = activeRecords.stream()
                .mapToInt(SalaryRecord::getMiniCoins)
                .sum();
        double usageRate = SALARY_POOL_TOTAL > 0 ? (double) allocated / SALARY_POOL_TOTAL : 0.0;

        // Build ranking sorted by miniCoins descending
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<MemberSalaryRank> ranking = activeRecords.stream()
                .sorted(Comparator.comparingInt(SalaryRecord::getMiniCoins).reversed())
                .map(record -> MemberSalaryRank.builder()
                        .userId(record.getUserId())
                        .username(Optional.ofNullable(userMap.get(record.getUserId()))
                                .map(User::getUsername)
                                .orElse("unknown"))
                        .totalPoints(record.getTotalPoints())
                        .miniCoins(record.getMiniCoins())
                        .build())
                .toList();

        return SalaryStatsDTO.builder()
                .totalPool(SALARY_POOL_TOTAL)
                .allocated(allocated)
                .usageRate(usageRate)
                .ranking(ranking)
                .build();
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_DASHBOARD, key = "'operations'")
    public OperationsDataDTO getOperationsData() {
        // 用户增长趋势：最近12个月按月统计新增用户数
        List<MonthlyGrowthDTO> userGrowthTrend = buildUserGrowthTrend();

        // 问题处理效率：已处理申请数 vs 总申请数
        Map<String, Object> issueProcessingStats = buildIssueProcessingStats();

        return OperationsDataDTO.builder()
                .userGrowthTrend(userGrowthTrend)
                .issueProcessingStats(issueProcessingStats)
                .build();
    }

    private List<MonthlyGrowthDTO> buildUserGrowthTrend() {
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<User> recentUsers = userRepository.findByCreatedAtAfter(twelveMonthsAgo);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // Group users by year-month
        Map<String, Long> monthCounts = recentUsers.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().format(formatter),
                        Collectors.counting()
                ));

        // Build a complete list of the last 12 months (including months with 0 users)
        List<MonthlyGrowthDTO> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String monthKey = ym.format(formatter);
            long count = monthCounts.getOrDefault(monthKey, 0L);
            trend.add(MonthlyGrowthDTO.builder().month(monthKey).count(count).build());
        }

        return trend;
    }

    private Map<String, Object> buildIssueProcessingStats() {
        long totalApplications = applicationRepository.count();
        long pendingApplications = applicationRepository.countByStatus(ApplicationStatus.PENDING_INITIAL_REVIEW);
        long processedApplications = totalApplications - pendingApplications;
        double processingRate = totalApplications > 0 ? (double) processedApplications / totalApplications : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalApplications", totalApplications);
        stats.put("processedApplications", processedApplications);
        stats.put("pendingApplications", pendingApplications);
        stats.put("processingRate", processingRate);
        return stats;
    }
}
