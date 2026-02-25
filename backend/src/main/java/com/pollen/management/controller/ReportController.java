package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.entity.WeeklyReport;
import com.pollen.management.service.ExportService;
import com.pollen.management.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 报表与数据导出控制器
 * 权限：ADMIN、LEADER 可访问
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;

    /**
     * 获取周报列表
     * GET /api/reports/weekly
     */
    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<WeeklyReport>> listWeeklyReports() {
        List<WeeklyReport> reports = reportService.listWeeklyReports();
        return ApiResponse.success(reports);
    }

    /**
     * 获取周报详情
     * GET /api/reports/weekly/{id}
     */
    @GetMapping("/weekly/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<WeeklyReport> getWeeklyReport(@PathVariable Long id) {
        WeeklyReport report = reportService.getWeeklyReport(id);
        return ApiResponse.success(report);
    }

    /**
     * 手动触发生成周报
     * POST /api/reports/weekly/generate
     */
    @PostMapping("/weekly/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<WeeklyReport> generateWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        if (weekStart == null || weekEnd == null) {
            // Default: last week (previous Monday to previous Sunday)
            LocalDate today = LocalDate.now();
            weekStart = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            weekEnd = weekStart.plusDays(6);
        }
        WeeklyReport report = reportService.generateWeeklyReport(weekStart, weekEnd);
        return ApiResponse.success(report);
    }

    /**
     * 导出成员列表 Excel
     * GET /api/reports/export/members
     */
    @GetMapping("/export/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ResponseEntity<byte[]> exportMembers() {
        byte[] data = exportService.exportMembers();
        String fileName = exportService.generateFileName("members", null, null);
        return buildExcelResponse(data, fileName);
    }

    /**
     * 导出积分记录 Excel
     * GET /api/reports/export/points
     */
    @GetMapping("/export/points")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ResponseEntity<byte[]> exportPoints() {
        byte[] data = exportService.exportPoints();
        String fileName = exportService.generateFileName("points", null, null);
        return buildExcelResponse(data, fileName);
    }

    /**
     * 导出薪资记录 Excel
     * GET /api/reports/export/salary
     */
    @GetMapping("/export/salary")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ResponseEntity<byte[]> exportSalary() {
        byte[] data = exportService.exportSalary();
        String fileName = exportService.generateFileName("salary", null, null);
        return buildExcelResponse(data, fileName);
    }

    /**
     * 导出活动记录 Excel
     * GET /api/reports/export/activities
     */
    @GetMapping("/export/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ResponseEntity<byte[]> exportActivities() {
        byte[] data = exportService.exportActivities();
        String fileName = exportService.generateFileName("activities", null, null);
        return buildExcelResponse(data, fileName);
    }

    /**
     * 自定义时间段筛选导出
     * GET /api/reports/export/custom?dataType=members&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/export/custom")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ResponseEntity<byte[]> exportCustom(
            @RequestParam String dataType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] data = exportService.exportWithDateRange(dataType, startDate, endDate);
        String fileName = exportService.generateFileName(dataType, startDate, endDate);
        return buildExcelResponse(data, fileName);
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
