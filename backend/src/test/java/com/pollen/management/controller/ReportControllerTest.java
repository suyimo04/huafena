package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.entity.WeeklyReport;
import com.pollen.management.service.ExportService;
import com.pollen.management.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ReportController controller;

    // --- GET /api/reports/weekly ---

    @Test
    void listWeeklyReports_shouldReturnReportList() {
        List<WeeklyReport> reports = List.of(
                WeeklyReport.builder()
                        .id(1L).weekStart(LocalDate.of(2024, 6, 3)).weekEnd(LocalDate.of(2024, 6, 9))
                        .newApplications(5).interviewsCompleted(3).newMembers(1)
                        .activitiesHeld(2).totalPointsIssued(150)
                        .generatedAt(LocalDateTime.of(2024, 6, 10, 2, 0))
                        .build(),
                WeeklyReport.builder()
                        .id(2L).weekStart(LocalDate.of(2024, 5, 27)).weekEnd(LocalDate.of(2024, 6, 2))
                        .newApplications(3).interviewsCompleted(2).newMembers(0)
                        .activitiesHeld(1).totalPointsIssued(80)
                        .generatedAt(LocalDateTime.of(2024, 6, 3, 2, 0))
                        .build()
        );
        when(reportService.listWeeklyReports()).thenReturn(reports);

        ApiResponse<List<WeeklyReport>> response = controller.listWeeklyReports();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getNewApplications()).isEqualTo(5);
        verify(reportService).listWeeklyReports();
    }

    @Test
    void listWeeklyReports_empty_shouldReturnEmptyList() {
        when(reportService.listWeeklyReports()).thenReturn(List.of());

        ApiResponse<List<WeeklyReport>> response = controller.listWeeklyReports();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- GET /api/reports/weekly/{id} ---

    @Test
    void getWeeklyReport_shouldReturnReportDetail() {
        WeeklyReport report = WeeklyReport.builder()
                .id(1L).weekStart(LocalDate.of(2024, 6, 3)).weekEnd(LocalDate.of(2024, 6, 9))
                .newApplications(5).interviewsCompleted(3).newMembers(1)
                .activitiesHeld(2).totalPointsIssued(150)
                .generatedAt(LocalDateTime.of(2024, 6, 10, 2, 0))
                .build();
        when(reportService.getWeeklyReport(1L)).thenReturn(report);

        ApiResponse<WeeklyReport> response = controller.getWeeklyReport(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getWeekStart()).isEqualTo(LocalDate.of(2024, 6, 3));
        assertThat(response.getData().getTotalPointsIssued()).isEqualTo(150);
        verify(reportService).getWeeklyReport(1L);
    }

    @Test
    void getWeeklyReport_notFound_shouldPropagateException() {
        when(reportService.getWeeklyReport(999L))
                .thenThrow(new RuntimeException("周报不存在: 999"));

        assertThatThrownBy(() -> controller.getWeeklyReport(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("周报不存在: 999");
    }

    // --- POST /api/reports/weekly/generate ---

    @Test
    void generateWeeklyReport_withDates_shouldGenerateForSpecifiedRange() {
        LocalDate start = LocalDate.of(2024, 6, 3);
        LocalDate end = LocalDate.of(2024, 6, 9);
        WeeklyReport report = WeeklyReport.builder()
                .id(1L).weekStart(start).weekEnd(end)
                .newApplications(5).interviewsCompleted(3).newMembers(1)
                .activitiesHeld(2).totalPointsIssued(150)
                .generatedAt(LocalDateTime.now())
                .build();
        when(reportService.generateWeeklyReport(start, end)).thenReturn(report);

        ApiResponse<WeeklyReport> response = controller.generateWeeklyReport(start, end);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getWeekStart()).isEqualTo(start);
        verify(reportService).generateWeeklyReport(start, end);
    }

    @Test
    void generateWeeklyReport_withoutDates_shouldDefaultToLastWeek() {
        // When no dates provided, controller defaults to last week
        ApiResponse<WeeklyReport> response = controller.generateWeeklyReport(null, null);

        assertThat(response.getCode()).isEqualTo(200);
        // Verify that generateWeeklyReport was called with some dates (last week)
        verify(reportService).generateWeeklyReport(any(LocalDate.class), any(LocalDate.class));
    }

    // --- GET /api/reports/export/members ---

    @Test
    void exportMembers_shouldReturnExcelFile() {
        byte[] data = new byte[]{1, 2, 3};
        when(exportService.exportMembers()).thenReturn(data);
        when(exportService.generateFileName("members", null, null)).thenReturn("members_20240610.xlsx");

        ResponseEntity<byte[]> response = controller.exportMembers();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(data);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("members_20240610.xlsx");
        verify(exportService).exportMembers();
    }

    // --- GET /api/reports/export/points ---

    @Test
    void exportPoints_shouldReturnExcelFile() {
        byte[] data = new byte[]{4, 5, 6};
        when(exportService.exportPoints()).thenReturn(data);
        when(exportService.generateFileName("points", null, null)).thenReturn("points_20240610.xlsx");

        ResponseEntity<byte[]> response = controller.exportPoints();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(data);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("points_20240610.xlsx");
    }

    // --- GET /api/reports/export/salary ---

    @Test
    void exportSalary_shouldReturnExcelFile() {
        byte[] data = new byte[]{7, 8, 9};
        when(exportService.exportSalary()).thenReturn(data);
        when(exportService.generateFileName("salary", null, null)).thenReturn("salary_20240610.xlsx");

        ResponseEntity<byte[]> response = controller.exportSalary();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(data);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("salary_20240610.xlsx");
    }

    // --- GET /api/reports/export/activities ---

    @Test
    void exportActivities_shouldReturnExcelFile() {
        byte[] data = new byte[]{10, 11, 12};
        when(exportService.exportActivities()).thenReturn(data);
        when(exportService.generateFileName("activities", null, null)).thenReturn("activities_20240610.xlsx");

        ResponseEntity<byte[]> response = controller.exportActivities();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(data);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("activities_20240610.xlsx");
    }

    // --- GET /api/reports/export/custom ---

    @Test
    void exportCustom_shouldReturnFilteredExcelFile() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        byte[] data = new byte[]{13, 14, 15};
        when(exportService.exportWithDateRange("members", start, end)).thenReturn(data);
        when(exportService.generateFileName("members", start, end)).thenReturn("members_20240101_20240131.xlsx");

        ResponseEntity<byte[]> response = controller.exportCustom("members", start, end);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(data);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("members_20240101_20240131.xlsx");
        verify(exportService).exportWithDateRange("members", start, end);
    }

    @Test
    void exportCustom_invalidDataType_shouldPropagateException() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(exportService.exportWithDateRange("invalid", start, end))
                .thenThrow(new IllegalArgumentException("Unsupported data type: invalid"));

        assertThatThrownBy(() -> controller.exportCustom("invalid", start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported data type: invalid");
    }
}
