package com.pollen.management.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyReportEntityTest {

    @Test
    void builderShouldCreateWeeklyReportWithDefaults() {
        LocalDate start = LocalDate.of(2024, 6, 3);
        LocalDate end = LocalDate.of(2024, 6, 9);

        WeeklyReport report = WeeklyReport.builder()
                .weekStart(start)
                .weekEnd(end)
                .build();

        assertNull(report.getId());
        assertEquals(start, report.getWeekStart());
        assertEquals(end, report.getWeekEnd());
        assertEquals(0, report.getNewApplications());
        assertEquals(0, report.getInterviewsCompleted());
        assertEquals(0, report.getNewMembers());
        assertEquals(0, report.getActivitiesHeld());
        assertEquals(0, report.getTotalPointsIssued());
        assertNull(report.getDetailData());
        assertNull(report.getGeneratedAt());
    }

    @Test
    void builderShouldAcceptAllFields() {
        LocalDate start = LocalDate.of(2024, 6, 3);
        LocalDate end = LocalDate.of(2024, 6, 9);
        LocalDateTime generatedAt = LocalDateTime.of(2024, 6, 10, 2, 0, 0);
        String detailJson = "{\"topContributor\":\"user1\"}";

        WeeklyReport report = WeeklyReport.builder()
                .id(1L)
                .weekStart(start)
                .weekEnd(end)
                .newApplications(5)
                .interviewsCompleted(3)
                .newMembers(2)
                .activitiesHeld(1)
                .totalPointsIssued(150)
                .detailData(detailJson)
                .generatedAt(generatedAt)
                .build();

        assertEquals(1L, report.getId());
        assertEquals(start, report.getWeekStart());
        assertEquals(end, report.getWeekEnd());
        assertEquals(5, report.getNewApplications());
        assertEquals(3, report.getInterviewsCompleted());
        assertEquals(2, report.getNewMembers());
        assertEquals(1, report.getActivitiesHeld());
        assertEquals(150, report.getTotalPointsIssued());
        assertEquals(detailJson, report.getDetailData());
        assertEquals(generatedAt, report.getGeneratedAt());
    }

    @Test
    void onCreateShouldSetGeneratedAtWhenNull() {
        WeeklyReport report = WeeklyReport.builder()
                .weekStart(LocalDate.now())
                .weekEnd(LocalDate.now().plusDays(6))
                .build();

        assertNull(report.getGeneratedAt());
        report.onCreate();
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    void onCreateShouldNotOverrideExistingGeneratedAt() {
        LocalDateTime preset = LocalDateTime.of(2024, 1, 1, 0, 0);
        WeeklyReport report = WeeklyReport.builder()
                .weekStart(LocalDate.now())
                .weekEnd(LocalDate.now().plusDays(6))
                .generatedAt(preset)
                .build();

        report.onCreate();
        assertEquals(preset, report.getGeneratedAt());
    }

    @Test
    void settersShouldWork() {
        WeeklyReport report = new WeeklyReport();
        report.setWeekStart(LocalDate.of(2024, 7, 1));
        report.setWeekEnd(LocalDate.of(2024, 7, 7));
        report.setNewApplications(10);
        report.setInterviewsCompleted(8);
        report.setNewMembers(4);
        report.setActivitiesHeld(2);
        report.setTotalPointsIssued(300);
        report.setDetailData("{\"key\":\"value\"}");

        assertEquals(LocalDate.of(2024, 7, 1), report.getWeekStart());
        assertEquals(LocalDate.of(2024, 7, 7), report.getWeekEnd());
        assertEquals(10, report.getNewApplications());
        assertEquals(8, report.getInterviewsCompleted());
        assertEquals(4, report.getNewMembers());
        assertEquals(2, report.getActivitiesHeld());
        assertEquals(300, report.getTotalPointsIssued());
        assertEquals("{\"key\":\"value\"}", report.getDetailData());
    }
}
