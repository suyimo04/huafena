package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.service.SalaryService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryControllerTest {

    @Mock
    private SalaryService salaryService;

    @InjectMocks
    private SalaryController controller;

    // --- POST /api/salary/calculate ---

    @Test
    void calculateSalaries_shouldDelegateToServiceAndReturnSuccess() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).totalPoints(200).miniCoins(400).build(),
                SalaryRecord.builder().id(2L).userId(11L).totalPoints(150).miniCoins(300).build()
        );
        when(salaryService.calculateSalaries()).thenReturn(records);

        ApiResponse<List<SalaryRecord>> response = controller.calculateSalaries();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getUserId()).isEqualTo(10L);
        verify(salaryService).calculateSalaries();
    }

    @Test
    void calculateSalaries_noFormalMembers_shouldPropagateException() {
        when(salaryService.calculateSalaries())
                .thenThrow(new BusinessException(400, "正式成员数量不足"));

        assertThatThrownBy(() -> controller.calculateSalaries())
                .isInstanceOf(BusinessException.class)
                .hasMessage("正式成员数量不足");
    }

    // --- POST /api/salary/calculate-distribute ---

    @Test
    void calculateAndDistribute_shouldDelegateToServiceAndReturnSuccess() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).totalPoints(200).miniCoins(400)
                        .communityActivityPoints(80).checkinCount(45).checkinPoints(30).build(),
                SalaryRecord.builder().id(2L).userId(11L).totalPoints(150).miniCoins(300)
                        .communityActivityPoints(60).checkinCount(35).checkinPoints(0).build()
        );
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.calculateAndDistribute("2025-07")).thenReturn(records);

        ApiResponse<List<SalaryRecord>> response = controller.calculateAndDistribute(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getCommunityActivityPoints()).isEqualTo(80);
        assertThat(response.getData().get(0).getCheckinPoints()).isEqualTo(30);
        verify(salaryService).calculateAndDistribute("2025-07");
    }

    @Test
    void calculateAndDistribute_noRecords_shouldPropagateException() {
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.calculateAndDistribute("2025-07"))
                .thenThrow(new BusinessException(404, "当前没有未归档的薪资记录"));

        assertThatThrownBy(() -> controller.calculateAndDistribute(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前没有未归档的薪资记录");
    }

    // --- GET /api/salary/list ---

    @Test
    void getSalaryList_shouldReturnAllRecords() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).basePoints(100).bonusPoints(20)
                        .deductions(5).totalPoints(115).miniCoins(230)
                        .salaryAmount(BigDecimal.valueOf(230)).remark("正常").build(),
                SalaryRecord.builder().id(2L).userId(11L).basePoints(80).bonusPoints(10)
                        .deductions(0).totalPoints(90).miniCoins(180)
                        .salaryAmount(BigDecimal.valueOf(180)).build()
        );
        when(salaryService.getSalaryList()).thenReturn(records);

        ApiResponse<List<SalaryRecord>> response = controller.getSalaryList();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getBasePoints()).isEqualTo(100);
        assertThat(response.getData().get(0).getRemark()).isEqualTo("正常");
        verify(salaryService).getSalaryList();
    }

    @Test
    void getSalaryList_empty_shouldReturnEmptyList() {
        when(salaryService.getSalaryList()).thenReturn(Collections.emptyList());

        ApiResponse<List<SalaryRecord>> response = controller.getSalaryList();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- PUT /api/salary/{id} ---

    @Test
    void updateSalaryRecord_shouldDelegateToServiceAndReturnUpdated() {
        SalaryRecord updates = SalaryRecord.builder()
                .bonusPoints(30).remark("绩效奖励").build();
        SalaryRecord updated = SalaryRecord.builder()
                .id(1L).userId(10L).basePoints(100).bonusPoints(30)
                .deductions(0).totalPoints(130).miniCoins(260)
                .salaryAmount(BigDecimal.valueOf(260)).remark("绩效奖励").build();
        when(salaryService.updateSalaryRecord(1L, updates)).thenReturn(updated);

        ApiResponse<SalaryRecord> response = controller.updateSalaryRecord(1L, updates);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getBonusPoints()).isEqualTo(30);
        assertThat(response.getData().getRemark()).isEqualTo("绩效奖励");
        verify(salaryService).updateSalaryRecord(1L, updates);
    }

    @Test
    void updateSalaryRecord_notFound_shouldPropagateException() {
        SalaryRecord updates = SalaryRecord.builder().bonusPoints(10).build();
        when(salaryService.updateSalaryRecord(99L, updates))
                .thenThrow(new BusinessException(404, "薪资记录不存在"));

        assertThatThrownBy(() -> controller.updateSalaryRecord(99L, updates))
                .isInstanceOf(BusinessException.class)
                .hasMessage("薪资记录不存在");
    }

    // --- POST /api/salary/batch-save ---

    @Test
    void batchSave_validRecords_shouldReturnSuccessResponse() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).miniCoins(400).build(),
                SalaryRecord.builder().id(2L).userId(11L).miniCoins(400).build(),
                SalaryRecord.builder().id(3L).userId(12L).miniCoins(400).build(),
                SalaryRecord.builder().id(4L).userId(13L).miniCoins(400).build(),
                SalaryRecord.builder().id(5L).userId(14L).miniCoins(400).build()
        );
        BatchSaveRequest request = BatchSaveRequest.builder()
                .records(records).operatorId(1L).build();
        BatchSaveResponse serviceResponse = BatchSaveResponse.builder()
                .success(true).savedRecords(records).build();
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.batchSaveWithValidation(records, 1L, "2025-07")).thenReturn(serviceResponse);

        ApiResponse<BatchSaveResponse> response = controller.batchSave(request, null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().isSuccess()).isTrue();
        assertThat(response.getData().getSavedRecords()).hasSize(5);
        verify(salaryService).batchSaveWithValidation(records, 1L, "2025-07");
    }

    @Test
    void batchSave_validationFails_shouldReturnErrorResponse() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).miniCoins(500).build()
        );
        BatchSaveRequest request = BatchSaveRequest.builder()
                .records(records).operatorId(1L).build();
        BatchSaveResponse serviceResponse = BatchSaveResponse.builder()
                .success(false)
                .globalError("单个成员迷你币不在 200-400 范围内")
                .violatingUserIds(List.of(10L))
                .errors(List.of(BatchSaveResponse.ValidationError.builder()
                        .userId(10L).field("miniCoins").message("迷你币 500 不在 [200, 400] 范围内").build()))
                .build();
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.batchSaveWithValidation(records, 1L, "2025-07")).thenReturn(serviceResponse);

        ApiResponse<BatchSaveResponse> response = controller.batchSave(request, null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().isSuccess()).isFalse();
        assertThat(response.getData().getGlobalError()).contains("200-400");
        assertThat(response.getData().getViolatingUserIds()).containsExactly(10L);
        assertThat(response.getData().getErrors()).hasSize(1);
    }

    // --- GET /api/salary/report ---

    @Test
    void generateReport_shouldReturnSalaryReport() {
        SalaryReportDTO report = SalaryReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .salaryPoolTotal(2000)
                .allocatedTotal(1800)
                .details(List.of(
                        SalaryReportDTO.MemberSalaryDetail.builder()
                                .userId(10L).username("member1").role("MEMBER")
                                .basePoints(100).bonusPoints(20).deductions(5)
                                .totalPoints(115).miniCoins(230)
                                .salaryAmount(BigDecimal.valueOf(230)).build()
                ))
                .build();
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.generateSalaryReport("2025-07")).thenReturn(report);

        ApiResponse<SalaryReportDTO> response = controller.generateReport(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getSalaryPoolTotal()).isEqualTo(2000);
        assertThat(response.getData().getAllocatedTotal()).isEqualTo(1800);
        assertThat(response.getData().getDetails()).hasSize(1);
        assertThat(response.getData().getDetails().get(0).getUsername()).isEqualTo("member1");
        verify(salaryService).generateSalaryReport("2025-07");
    }

    @Test
    void generateReport_noRecords_shouldReturnEmptyReport() {
        SalaryReportDTO report = SalaryReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .salaryPoolTotal(2000)
                .allocatedTotal(0)
                .details(Collections.emptyList())
                .build();
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.generateSalaryReport("2025-07")).thenReturn(report);

        ApiResponse<SalaryReportDTO> response = controller.generateReport(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getAllocatedTotal()).isEqualTo(0);
        assertThat(response.getData().getDetails()).isEmpty();
    }

    // --- POST /api/salary/archive ---

    @Test
    void archiveSalaryRecords_shouldReturnArchivedCount() {
        ArchiveRequest request = ArchiveRequest.builder().operatorId(1L).build();
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.archiveSalaryRecords(1L, "2025-07")).thenReturn(5);

        ApiResponse<Integer> response = controller.archiveSalaryRecords(request, null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(5);
        verify(salaryService).archiveSalaryRecords(1L, "2025-07");
    }

    @Test
    void archiveSalaryRecords_noRecordsToArchive_shouldReturnZero() {
        ArchiveRequest request = ArchiveRequest.builder().operatorId(1L).build();
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.archiveSalaryRecords(1L, "2025-07")).thenReturn(0);

        ApiResponse<Integer> response = controller.archiveSalaryRecords(request, null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(0);
    }


    // --- GET /api/salary/periods ---

    @Test
    void getPeriods_shouldReturnPeriodListFromService() {
        List<SalaryPeriodDTO> periods = List.of(
                SalaryPeriodDTO.builder().period("2025-07").archived(false).recordCount(5).build(),
                SalaryPeriodDTO.builder().period("2025-06").archived(true).recordCount(4).build()
        );
        when(salaryService.getPeriodList()).thenReturn(periods);

        ApiResponse<List<SalaryPeriodDTO>> response = controller.getPeriods();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getPeriod()).isEqualTo("2025-07");
        assertThat(response.getData().get(0).isArchived()).isFalse();
        assertThat(response.getData().get(0).getRecordCount()).isEqualTo(5);
        assertThat(response.getData().get(1).getPeriod()).isEqualTo("2025-06");
        assertThat(response.getData().get(1).isArchived()).isTrue();
        verify(salaryService).getPeriodList();
    }

    @Test
    void getPeriods_empty_shouldReturnEmptyList() {
        when(salaryService.getPeriodList()).thenReturn(Collections.emptyList());

        ApiResponse<List<SalaryPeriodDTO>> response = controller.getPeriods();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- POST /api/salary/periods ---

    @Test
    void createPeriod_shouldDelegateToServiceAndReturnRecords() {
        CreatePeriodRequest request = CreatePeriodRequest.builder().period("2025-08").build();
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).period("2025-08").totalPoints(0).miniCoins(0).build(),
                SalaryRecord.builder().id(2L).userId(11L).period("2025-08").totalPoints(0).miniCoins(0).build()
        );
        when(salaryService.createPeriod("2025-08")).thenReturn(records);

        ApiResponse<List<SalaryRecord>> response = controller.createPeriod(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getPeriod()).isEqualTo("2025-08");
        assertThat(response.getData().get(0).getTotalPoints()).isEqualTo(0);
        verify(salaryService).createPeriod("2025-08");
    }

    @Test
    void createPeriod_duplicatePeriod_shouldPropagateException() {
        CreatePeriodRequest request = CreatePeriodRequest.builder().period("2025-07").build();
        when(salaryService.createPeriod("2025-07"))
                .thenThrow(new BusinessException(409, "周期 2025-07 已存在"));

        assertThatThrownBy(() -> controller.createPeriod(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("周期 2025-07 已存在");
    }

    // --- GET /api/salary/members with period parameter ---

    @Test
    void getSalaryMembers_withoutPeriod_shouldUseLatestActivePeriod() {
        List<SalaryMemberDTO> members = List.of(
                SalaryMemberDTO.builder().userId(10L).username("member1").role("MEMBER").totalPoints(100).build()
        );
        when(salaryService.getLatestActivePeriod()).thenReturn("2025-07");
        when(salaryService.getSalaryMembers("2025-07")).thenReturn(members);

        ApiResponse<List<SalaryMemberDTO>> response = controller.getSalaryMembers(null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getUsername()).isEqualTo("member1");
        verify(salaryService).getLatestActivePeriod();
        verify(salaryService).getSalaryMembers("2025-07");
    }

    @Test
    void getSalaryMembers_withPeriod_shouldPassPeriodDirectly() {
        List<SalaryMemberDTO> members = List.of(
                SalaryMemberDTO.builder().userId(10L).username("member1").role("MEMBER").totalPoints(80).build()
        );
        when(salaryService.getSalaryMembers("2025-06")).thenReturn(members);

        ApiResponse<List<SalaryMemberDTO>> response = controller.getSalaryMembers("2025-06");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        verify(salaryService, never()).getLatestActivePeriod();
        verify(salaryService).getSalaryMembers("2025-06");
    }

    // --- Period pass-through tests ---

    @Test
    void calculateAndDistribute_withExplicitPeriod_shouldPassPeriodDirectly() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).period("2025-06").totalPoints(200).build()
        );
        when(salaryService.calculateAndDistribute("2025-06")).thenReturn(records);

        ApiResponse<List<SalaryRecord>> response = controller.calculateAndDistribute("2025-06");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        verify(salaryService, never()).getLatestActivePeriod();
        verify(salaryService).calculateAndDistribute("2025-06");
    }

    @Test
    void batchSave_withExplicitPeriod_shouldPassPeriodDirectly() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(10L).miniCoins(400).build()
        );
        BatchSaveRequest request = BatchSaveRequest.builder()
                .records(records).operatorId(1L).build();
        BatchSaveResponse serviceResponse = BatchSaveResponse.builder()
                .success(true).savedRecords(records).build();
        when(salaryService.batchSaveWithValidation(records, 1L, "2025-06")).thenReturn(serviceResponse);

        ApiResponse<BatchSaveResponse> response = controller.batchSave(request, "2025-06");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().isSuccess()).isTrue();
        verify(salaryService, never()).getLatestActivePeriod();
        verify(salaryService).batchSaveWithValidation(records, 1L, "2025-06");
    }

    @Test
    void archiveSalaryRecords_withExplicitPeriod_shouldPassPeriodDirectly() {
        ArchiveRequest request = ArchiveRequest.builder().operatorId(1L).build();
        when(salaryService.archiveSalaryRecords(1L, "2025-06")).thenReturn(3);

        ApiResponse<Integer> response = controller.archiveSalaryRecords(request, "2025-06");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(3);
        verify(salaryService, never()).getLatestActivePeriod();
        verify(salaryService).archiveSalaryRecords(1L, "2025-06");
    }

    @Test
    void generateReport_withExplicitPeriod_shouldPassPeriodDirectly() {
        SalaryReportDTO report = SalaryReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .salaryPoolTotal(2000)
                .allocatedTotal(1500)
                .details(Collections.emptyList())
                .build();
        when(salaryService.generateSalaryReport("2025-06")).thenReturn(report);

        ApiResponse<SalaryReportDTO> response = controller.generateReport("2025-06");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getAllocatedTotal()).isEqualTo(1500);
        verify(salaryService, never()).getLatestActivePeriod();
        verify(salaryService).generateSalaryReport("2025-06");
    }

}
