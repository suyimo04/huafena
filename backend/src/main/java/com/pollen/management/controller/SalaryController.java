package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.service.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 薪资管理控制器：计算、查看列表、编辑、批量保存、报告、归档端点
 * 基础权限：通过 SecurityConfig 配置（GET 允许 LEADER/VICE_LEADER/MEMBER，POST/PUT 仅 LEADER）
 * 细粒度权限：通过 @PreAuthorize 注解控制
 */
@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    /**
     * 计算所有正式成员的薪资
     * 仅 LEADER 可操作
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<SalaryRecord>> calculateSalaries() {
        List<SalaryRecord> records = salaryService.calculateSalaries();
        return ApiResponse.success(records);
    }

    /**
     * 整合维度计算 + 薪酬池分配
     * 基于已录入的维度明细数据，自动计算各维度积分并执行薪酬池分配
     * 仅 ADMIN/LEADER 可操作
     */
    @PostMapping("/calculate-distribute")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<SalaryRecord>> calculateAndDistribute() {
        List<SalaryRecord> records = salaryService.calculateAndDistribute();
        return ApiResponse.success(records);
    }

    /**
     * 获取薪资列表
     * LEADER、VICE_LEADER、MEMBER 可查看
     */
    @GetMapping("/list")
    public ApiResponse<List<SalaryRecord>> getSalaryList() {
        List<SalaryRecord> records = salaryService.getSalaryList();
        return ApiResponse.success(records);
    }

    /**
     * 获取薪资管理成员列表（按角色分组，含用户名）
     * ADMIN、LEADER、VICE_LEADER 可查看
     */
    @GetMapping("/members")
    public ApiResponse<List<SalaryMemberDTO>> getSalaryMembers() {
        List<SalaryMemberDTO> members = salaryService.getSalaryMembers();
        return ApiResponse.success(members);
    }

    /**
     * 更新单条薪资记录（单元格编辑）
     * 仅 LEADER 可操作
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<SalaryRecord> updateSalaryRecord(
            @PathVariable Long id,
            @RequestBody SalaryRecord updates) {
        SalaryRecord updated = salaryService.updateSalaryRecord(id, updates);
        return ApiResponse.success(updated);
    }

    /**
     * 批量保存薪资记录（带验证）
     * 仅 LEADER 可操作
     */
    @PostMapping("/batch-save")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<BatchSaveResponse> batchSave(@Valid @RequestBody BatchSaveRequest request) {
        BatchSaveResponse response = salaryService.batchSaveWithValidation(
                request.getRecords(), request.getOperatorId());
        return ApiResponse.success(response);
    }

    /**
     * 生成薪酬明细报告
     * LEADER、VICE_LEADER、MEMBER 可查看
     */
    @GetMapping("/report")
    public ApiResponse<SalaryReportDTO> generateReport() {
        SalaryReportDTO report = salaryService.generateSalaryReport();
        return ApiResponse.success(report);
    }

    /**
     * 归档薪资记录
     * 仅 LEADER 可操作
     */
    @PostMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Integer> archiveSalaryRecords(@Valid @RequestBody ArchiveRequest request) {
        int archivedCount = salaryService.archiveSalaryRecords(request.getOperatorId());
        return ApiResponse.success(archivedCount);
    }
}
