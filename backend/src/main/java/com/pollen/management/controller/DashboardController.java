package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.DashboardStatsDTO;
import com.pollen.management.dto.OperationsDataDTO;
import com.pollen.management.dto.RecruitmentStatsDTO;
import com.pollen.management.dto.SalaryStatsDTO;
import com.pollen.management.entity.AuditLog;
import com.pollen.management.service.AuditLogService;
import com.pollen.management.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据看板与审计日志控制器
 * 权限控制：ADMIN、LEADER 可访问（通过 SecurityConfig /api/dashboard/** 配置）
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;

    /**
     * 获取数据看板统计指标
     * GET /api/dashboard/stats
     */
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ApiResponse.success(stats);
    }

    /**
     * 获取招募数据统计
     * GET /api/dashboard/recruitment
     */
    @GetMapping("/recruitment")
    public ApiResponse<RecruitmentStatsDTO> getRecruitmentStats() {
        RecruitmentStatsDTO stats = dashboardService.getRecruitmentStats();
        return ApiResponse.success(stats);
    }

    /**
     * 获取薪酬数据统计
     * GET /api/dashboard/salary
     */
    @GetMapping("/salary")
    public ApiResponse<SalaryStatsDTO> getSalaryStats() {
        SalaryStatsDTO stats = dashboardService.getSalaryStats();
        return ApiResponse.success(stats);
    }

    /**
     * 获取运营数据（用户增长趋势、问题处理效率）
     * GET /api/dashboard/operations
     */
    @GetMapping("/operations")
    public ApiResponse<OperationsDataDTO> getOperationsData() {
        OperationsDataDTO data = dashboardService.getOperationsData();
        return ApiResponse.success(data);
    }

    /**
     * 获取审计日志列表，支持按操作类型筛选
     * GET /api/dashboard/audit-logs
     * GET /api/dashboard/audit-logs?type=xxx
     */
    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String type) {
        List<AuditLog> logs;
        if (type != null && !type.isBlank()) {
            logs = auditLogService.getAuditLogsByType(type);
        } else {
            logs = auditLogService.getAuditLogs();
        }
        return ApiResponse.success(logs);
    }
}
