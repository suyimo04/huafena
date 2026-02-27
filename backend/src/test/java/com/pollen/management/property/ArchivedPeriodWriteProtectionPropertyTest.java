package com.pollen.management.property;

import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.PointsService;
import com.pollen.management.service.SalaryConfigService;
import com.pollen.management.service.SalaryServiceImpl;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: salary-period, Property 8: 已归档周期写保护
 * **Validates: Requirements 4.2, 6.3**
 *
 * For any 已归档的周期，尝试对该周期执行计算、保存或编辑操作应被拒绝并返回错误信息。
 */
class ArchivedPeriodWriteProtectionPropertyTest {

    private SalaryServiceImpl createService(SalaryRecordRepository salaryRecordRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository,
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                mock(SalaryConfigService.class));
    }

    // ========================================================================
    // Property 8: 已归档周期写保护 — calculateAndDistribute 应被拒绝
    // **Validates: Requirements 4.2, 6.3**
    // ========================================================================

    @Property(tries = 100)
    void calculateAndDistributeRejectsArchivedPeriod(
            @ForAll("validPeriods") String period) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        when(salaryRepo.existsByPeriodAndArchivedTrue(anyString())).thenReturn(true);

        SalaryServiceImpl service = createService(salaryRepo);

        assertThatThrownBy(() -> service.calculateAndDistribute(period))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(400);
                });
    }

    // ========================================================================
    // Property 8: 已归档周期写保护 — batchSaveWithValidation 应被拒绝
    // **Validates: Requirements 4.2, 6.3**
    // ========================================================================

    @Property(tries = 100)
    void batchSaveWithValidationRejectsArchivedPeriod(
            @ForAll("validPeriods") String period,
            @ForAll("salaryRecordLists") List<SalaryRecord> records) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        when(salaryRepo.existsByPeriodAndArchivedTrue(anyString())).thenReturn(true);

        SalaryServiceImpl service = createService(salaryRepo);

        assertThatThrownBy(() -> service.batchSaveWithValidation(records, 1L, period))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(400);
                });
    }

    // ========================================================================
    // Property 8: 已归档周期写保护 — archiveSalaryRecords 应被拒绝
    // **Validates: Requirements 4.2, 6.3**
    // ========================================================================

    @Property(tries = 100)
    void archiveSalaryRecordsRejectsArchivedPeriod(
            @ForAll("validPeriods") String period) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        when(salaryRepo.existsByPeriodAndArchivedTrue(anyString())).thenReturn(true);

        SalaryServiceImpl service = createService(salaryRepo);

        assertThatThrownBy(() -> service.archiveSalaryRecords(1L, period))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(400);
                });
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<String> validPeriods() {
        return Combinators.combine(
                Arbitraries.integers().between(2000, 2099),
                Arbitraries.integers().between(1, 12)
        ).as((year, month) -> String.format("%04d-%02d", year, month));
    }

    @Provide
    Arbitrary<List<SalaryRecord>> salaryRecordLists() {
        return salaryRecord().list().ofMinSize(1).ofMaxSize(10);
    }

    private Arbitrary<SalaryRecord> salaryRecord() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 5000),
                Arbitraries.integers().between(0, 100),
                Arbitraries.integers().between(0, 300)
        ).as((userId, basePoints, totalPoints) ->
                SalaryRecord.builder()
                        .userId(userId)
                        .period("2025-01")
                        .basePoints(basePoints)
                        .bonusPoints(0)
                        .deductions(0)
                        .totalPoints(totalPoints)
                        .miniCoins(0)
                        .salaryAmount(BigDecimal.ZERO)
                        .archived(false)
                        .build());
    }
}
