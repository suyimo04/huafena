package com.pollen.management.service;

import com.pollen.management.entity.WeeklyReport;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final RoleChangeHistoryRepository roleChangeHistoryRepository;
    private final ActivityRepository activityRepository;
    private final PointsRecordRepository pointsRecordRepository;

    @Override
    public WeeklyReport generateWeeklyReport(LocalDate weekStart, LocalDate weekEnd) {
        // Check if report already exists for this week
        return weeklyReportRepository.findByWeekStartAndWeekEnd(weekStart, weekEnd)
                .orElseGet(() -> createWeeklyReport(weekStart, weekEnd));
    }

    @Override
    public List<WeeklyReport> listWeeklyReports() {
        return weeklyReportRepository.findAllByOrderByWeekStartDesc();
    }

    @Override
    public WeeklyReport getWeeklyReport(Long id) {
        return weeklyReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("周报不存在: " + id));
    }

    /**
     * 每周一凌晨 2 点自动生成上一周的周报
     */
    @Scheduled(cron = "0 0 2 ? * MON")
    public void autoGenerateWeeklyReport() {
        LocalDate today = LocalDate.now();
        // Last week: previous Monday to previous Sunday
        LocalDate lastMonday = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        LocalDate lastSunday = lastMonday.plusDays(6);

        log.info("自动生成周报: {} ~ {}", lastMonday, lastSunday);
        generateWeeklyReport(lastMonday, lastSunday);
    }

    private WeeklyReport createWeeklyReport(LocalDate weekStart, LocalDate weekEnd) {
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = weekEnd.atTime(LocalTime.MAX);

        int newApplications = (int) applicationRepository.countByCreatedAtBetween(rangeStart, rangeEnd);
        int interviewsCompleted = (int) interviewRepository.countByCompletedAtBetween(rangeStart, rangeEnd);
        int newMembers = (int) roleChangeHistoryRepository.countByNewRoleAndChangedAtBetween(
                Role.MEMBER, rangeStart, rangeEnd);
        int activitiesHeld = (int) activityRepository.countByActivityTimeBetween(rangeStart, rangeEnd);
        int totalPointsIssued = pointsRecordRepository.sumPositiveAmountByCreatedAtBetween(rangeStart, rangeEnd);

        WeeklyReport report = WeeklyReport.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .newApplications(newApplications)
                .interviewsCompleted(interviewsCompleted)
                .newMembers(newMembers)
                .activitiesHeld(activitiesHeld)
                .totalPointsIssued(totalPointsIssued)
                .build();

        return weeklyReportRepository.save(report);
    }
}
