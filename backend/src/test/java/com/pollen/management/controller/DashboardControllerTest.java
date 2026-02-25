package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.AuditLog;
import com.pollen.management.service.AuditLogService;
import com.pollen.management.service.DashboardService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DashboardController controller;

    // --- GET /api/dashboard/stats ---

    @Test
    void getDashboardStats_shouldReturnStatsFromService() {
        var stats = DashboardStatsDTO.builder()
                .totalMembers(20)
                .adminCount(1)
                .leaderCount(1)
                .viceLeaderCount(1)
                .memberCount(4)
                .internCount(3)
                .applicantCount(10)
                .totalActivities(5)
                .totalPointsRecords(100)
                .build();
        when(dashboardService.getDashboardStats()).thenReturn(stats);

        ApiResponse<DashboardStatsDTO> response = controller.getDashboardStats();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData().getTotalMembers()).isEqualTo(20);
        assertThat(response.getData().getAdminCount()).isEqualTo(1);
        assertThat(response.getData().getLeaderCount()).isEqualTo(1);
        assertThat(response.getData().getViceLeaderCount()).isEqualTo(1);
        assertThat(response.getData().getMemberCount()).isEqualTo(4);
        assertThat(response.getData().getInternCount()).isEqualTo(3);
        assertThat(response.getData().getApplicantCount()).isEqualTo(10);
        assertThat(response.getData().getTotalActivities()).isEqualTo(5);
        assertThat(response.getData().getTotalPointsRecords()).isEqualTo(100);
        verify(dashboardService).getDashboardStats();
    }

    @Test
    void getDashboardStats_withZeroCounts_shouldReturnZeroStats() {
        var stats = DashboardStatsDTO.builder()
                .totalMembers(0)
                .adminCount(0)
                .leaderCount(0)
                .viceLeaderCount(0)
                .memberCount(0)
                .internCount(0)
                .applicantCount(0)
                .totalActivities(0)
                .totalPointsRecords(0)
                .build();
        when(dashboardService.getDashboardStats()).thenReturn(stats);

        ApiResponse<DashboardStatsDTO> response = controller.getDashboardStats();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTotalMembers()).isEqualTo(0);
        assertThat(response.getData().getTotalActivities()).isEqualTo(0);
    }

    // --- GET /api/dashboard/audit-logs ---

    @Test
    void getAuditLogs_withoutType_shouldReturnAllLogs() {
        var log1 = AuditLog.builder()
                .id(1L)
                .operatorId(1L)
                .operationType("薪资保存")
                .operationTime(LocalDateTime.of(2025, 7, 10, 10, 0))
                .operationDetail("批量保存薪资记录")
                .build();
        var log2 = AuditLog.builder()
                .id(2L)
                .operatorId(2L)
                .operationType("角色变更")
                .operationTime(LocalDateTime.of(2025, 7, 9, 15, 30))
                .operationDetail("用户3角色从INTERN变更为MEMBER")
                .build();
        when(auditLogService.getAuditLogs()).thenReturn(List.of(log1, log2));

        ApiResponse<List<AuditLog>> response = controller.getAuditLogs(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getOperationType()).isEqualTo("薪资保存");
        assertThat(response.getData().get(1).getOperationType()).isEqualTo("角色变更");
        verify(auditLogService).getAuditLogs();
        verify(auditLogService, never()).getAuditLogsByType(anyString());
    }

    @Test
    void getAuditLogs_withBlankType_shouldReturnAllLogs() {
        when(auditLogService.getAuditLogs()).thenReturn(Collections.emptyList());

        ApiResponse<List<AuditLog>> response = controller.getAuditLogs("   ");

        assertThat(response.getCode()).isEqualTo(200);
        verify(auditLogService).getAuditLogs();
        verify(auditLogService, never()).getAuditLogsByType(anyString());
    }

    @Test
    void getAuditLogs_withType_shouldReturnFilteredLogs() {
        var log = AuditLog.builder()
                .id(1L)
                .operatorId(1L)
                .operationType("薪资保存")
                .operationTime(LocalDateTime.of(2025, 7, 10, 10, 0))
                .operationDetail("批量保存薪资记录")
                .build();
        when(auditLogService.getAuditLogsByType("薪资保存")).thenReturn(List.of(log));

        ApiResponse<List<AuditLog>> response = controller.getAuditLogs("薪资保存");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getOperationType()).isEqualTo("薪资保存");
        verify(auditLogService).getAuditLogsByType("薪资保存");
        verify(auditLogService, never()).getAuditLogs();
    }

    @Test
    void getAuditLogs_emptyResult_shouldReturnEmptyList() {
        when(auditLogService.getAuditLogs()).thenReturn(Collections.emptyList());

        ApiResponse<List<AuditLog>> response = controller.getAuditLogs(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void getAuditLogs_withTypeNoMatch_shouldReturnEmptyList() {
        when(auditLogService.getAuditLogsByType("不存在的类型")).thenReturn(Collections.emptyList());

        ApiResponse<List<AuditLog>> response = controller.getAuditLogs("不存在的类型");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
        verify(auditLogService).getAuditLogsByType("不存在的类型");
    }

    @Test
    void getAuditLogs_servicePropagatesException() {
        when(auditLogService.getAuditLogsByType("bad"))
                .thenThrow(new BusinessException(400, "操作类型不能为空"));

        assertThatThrownBy(() -> controller.getAuditLogs("bad"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("操作类型不能为空");
    }

    // --- GET /api/dashboard/recruitment ---

    @Test
    void getRecruitmentStats_shouldReturnStatsFromService() {
        var stats = RecruitmentStatsDTO.builder()
                .stageCount(Map.of("PENDING_INITIAL_REVIEW", 5L, "REJECTED", 2L))
                .aiInterviewPassRate(0.75)
                .manualReviewPassRate(0.6)
                .build();
        when(dashboardService.getRecruitmentStats()).thenReturn(stats);

        ApiResponse<RecruitmentStatsDTO> response = controller.getRecruitmentStats();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getStageCount()).containsEntry("PENDING_INITIAL_REVIEW", 5L);
        assertThat(response.getData().getAiInterviewPassRate()).isEqualTo(0.75);
        assertThat(response.getData().getManualReviewPassRate()).isEqualTo(0.6);
        verify(dashboardService).getRecruitmentStats();
    }

    @Test
    void getRecruitmentStats_withEmptyData_shouldReturnZeroRates() {
        var stats = RecruitmentStatsDTO.builder()
                .stageCount(Map.of())
                .aiInterviewPassRate(0.0)
                .manualReviewPassRate(0.0)
                .build();
        when(dashboardService.getRecruitmentStats()).thenReturn(stats);

        ApiResponse<RecruitmentStatsDTO> response = controller.getRecruitmentStats();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getAiInterviewPassRate()).isEqualTo(0.0);
        assertThat(response.getData().getManualReviewPassRate()).isEqualTo(0.0);
    }

    // --- GET /api/dashboard/salary ---

    @Test
    void getSalaryStats_shouldReturnStatsFromService() {
        var rank1 = MemberSalaryRank.builder().userId(1L).username("alice").totalPoints(200).miniCoins(400).build();
        var stats = SalaryStatsDTO.builder()
                .totalPool(2000)
                .allocated(400)
                .usageRate(0.2)
                .ranking(List.of(rank1))
                .build();
        when(dashboardService.getSalaryStats()).thenReturn(stats);

        ApiResponse<SalaryStatsDTO> response = controller.getSalaryStats();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTotalPool()).isEqualTo(2000);
        assertThat(response.getData().getAllocated()).isEqualTo(400);
        assertThat(response.getData().getUsageRate()).isEqualTo(0.2);
        assertThat(response.getData().getRanking()).hasSize(1);
        assertThat(response.getData().getRanking().get(0).getUsername()).isEqualTo("alice");
        verify(dashboardService).getSalaryStats();
    }

    @Test
    void getSalaryStats_withNoRecords_shouldReturnEmptyRanking() {
        var stats = SalaryStatsDTO.builder()
                .totalPool(2000)
                .allocated(0)
                .usageRate(0.0)
                .ranking(Collections.emptyList())
                .build();
        when(dashboardService.getSalaryStats()).thenReturn(stats);

        ApiResponse<SalaryStatsDTO> response = controller.getSalaryStats();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getAllocated()).isEqualTo(0);
        assertThat(response.getData().getRanking()).isEmpty();
    }
}
