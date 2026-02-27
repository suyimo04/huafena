package com.pollen.management.property;

import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.PointsService;
import com.pollen.management.service.SalaryConfigService;
import com.pollen.management.service.SalaryServiceImpl;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: salary-period, Property 7: 按周期归档完整性
 * **Validates: Requirements 4.1**
 *
 * For any 包含 N 条未归档记录的周期，执行归档操作后，
 * 该周期内所有 N 条记录的 archived 字段应为 true 且 archivedAt 应非空。
 */
class PeriodArchiveCompletenessPropertyTest {

    private SalaryServiceImpl createService(
            SalaryRecordRepository salaryRecordRepository,
            AuditLogRepository auditLogRepository) throws Exception {
        var ctor = SalaryServiceImpl.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (SalaryServiceImpl) ctor.newInstance(
                salaryRecordRepository,
                mock(UserRepository.class),
                mock(PointsService.class),
                auditLogRepository,
                mock(SalaryConfigService.class));
    }

    // ========================================================================
    // Property 7: 按周期归档完整性 — 归档返回值等于记录数 N
    // **Validates: Requirements 4.1**
    // ========================================================================

    @Property(tries = 100)
    void archiveReturnsCountEqualToNumberOfRecords(
            @ForAll("validPeriods") String period,
            @ForAll("unarchivedRecordLists") List<SalaryRecord> records) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        AuditLogRepository auditLogRepo = mock(AuditLogRepository.class);

        when(salaryRepo.existsByPeriodAndArchivedTrue(period)).thenReturn(false);
        when(salaryRepo.findByPeriodAndArchivedFalse(period)).thenReturn(records);
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryServiceImpl service = createService(salaryRepo, auditLogRepo);

        int result = service.archiveSalaryRecords(1L, period);

        assertThat(result).isEqualTo(records.size());
    }

    // ========================================================================
    // Property 7: 按周期归档完整性 — 所有记录 archived == true
    // **Validates: Requirements 4.1**
    // ========================================================================

    @Property(tries = 100)
    void allRecordsAreArchivedAfterArchiveOperation(
            @ForAll("validPeriods") String period,
            @ForAll("unarchivedRecordLists") List<SalaryRecord> records) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        AuditLogRepository auditLogRepo = mock(AuditLogRepository.class);

        when(salaryRepo.existsByPeriodAndArchivedTrue(period)).thenReturn(false);
        when(salaryRepo.findByPeriodAndArchivedFalse(period)).thenReturn(records);
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryServiceImpl service = createService(salaryRepo, auditLogRepo);

        service.archiveSalaryRecords(1L, period);

        assertThat(records).allSatisfy(record ->
                assertThat(record.getArchived())
                        .as("Record for userId=%d should have archived=true", record.getUserId())
                        .isTrue());
    }

    // ========================================================================
    // Property 7: 按周期归档完整性 — 所有记录 archivedAt 非空
    // **Validates: Requirements 4.1**
    // ========================================================================

    @Property(tries = 100)
    void allRecordsHaveNonNullArchivedAtAfterArchiveOperation(
            @ForAll("validPeriods") String period,
            @ForAll("unarchivedRecordLists") List<SalaryRecord> records) throws Exception {

        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        AuditLogRepository auditLogRepo = mock(AuditLogRepository.class);

        when(salaryRepo.existsByPeriodAndArchivedTrue(period)).thenReturn(false);
        when(salaryRepo.findByPeriodAndArchivedFalse(period)).thenReturn(records);
        when(salaryRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryServiceImpl service = createService(salaryRepo, auditLogRepo);

        service.archiveSalaryRecords(1L, period);

        assertThat(records).allSatisfy(record ->
                assertThat(record.getArchivedAt())
                        .as("Record for userId=%d should have non-null archivedAt", record.getUserId())
                        .isNotNull());
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
    Arbitrary<List<SalaryRecord>> unarchivedRecordLists() {
        return unarchivedRecord().list().ofMinSize(1).ofMaxSize(15);
    }

    private Arbitrary<SalaryRecord> unarchivedRecord() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 5000),
                Arbitraries.integers().between(0, 100),
                Arbitraries.integers().between(0, 300),
                Arbitraries.integers().between(0, 300),
                Arbitraries.integers().between(0, 800),
                Arbitraries.integers().between(0, 400)
        ).as((userId, basePoints, bonusPoints, deductions, totalPoints, miniCoins) ->
                SalaryRecord.builder()
                        .userId(userId)
                        .period("2025-01") // placeholder, overridden by mock
                        .basePoints(basePoints)
                        .bonusPoints(bonusPoints)
                        .deductions(deductions)
                        .totalPoints(totalPoints)
                        .miniCoins(miniCoins)
                        .salaryAmount(BigDecimal.valueOf(miniCoins))
                        .archived(false)
                        .archivedAt(null)
                        .build());
    }
}
