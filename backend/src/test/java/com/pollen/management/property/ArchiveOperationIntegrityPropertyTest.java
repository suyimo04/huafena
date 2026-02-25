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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for archive operation integrity.
 *
 * Feature: salary-calculation-rules, Property 10: 归档操作完整性
 * For any 一组未归档的薪资记录，执行归档操作后，所有记录的 archived 字段应为 true 且 archivedAt 应非空。
 *
 * **Validates: Requirements 7.4**
 */
class ArchiveOperationIntegrityPropertyTest {

    // ========================================================================
    // Property 10: 归档操作完整性
    // **Validates: Requirements 7.4**
    // ========================================================================

    @Property(tries = 100)
    void property10_afterArchiveAllRecordsAreArchivedWithTimestamp(
            @ForAll("unarchivedRecordList") List<SalaryRecord> records) {

        // Setup mocks
        SalaryRecordRepository salaryRecordRepository = mock(SalaryRecordRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        // saveAll returns the same list (records are mutated in-place by the service)
        when(salaryRecordRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryServiceImpl service = new SalaryServiceImpl(
                salaryRecordRepository,
                mock(UserRepository.class),
                mock(PointsService.class),
                auditLogRepository,
                mock(SalaryConfigService.class)
        );

        int archivedCount = service.archiveSalaryRecords(1L);

        // Verify: returned count matches the number of records
        assertThat(archivedCount).isEqualTo(records.size());

        // Verify: all records now have archived=true and archivedAt is non-null
        for (SalaryRecord record : records) {
            assertThat(record.getArchived())
                    .as("Record for userId=%d should be archived", record.getUserId())
                    .isTrue();
            assertThat(record.getArchivedAt())
                    .as("Record for userId=%d should have non-null archivedAt", record.getUserId())
                    .isNotNull();
        }
    }

    @Example
    void property10_archiveEmptyListReturnsZero() {
        // Edge case: no unarchived records
        SalaryRecordRepository salaryRecordRepository = mock(SalaryRecordRepository.class);
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(new ArrayList<>());

        SalaryServiceImpl service = new SalaryServiceImpl(
                salaryRecordRepository,
                mock(UserRepository.class),
                mock(PointsService.class),
                mock(AuditLogRepository.class),
                mock(SalaryConfigService.class)
        );

        int archivedCount = service.archiveSalaryRecords(1L);
        assertThat(archivedCount).isZero();
    }

    @Property(tries = 100)
    void property10_allRecordsShareSameArchivedAtTimestamp(
            @ForAll("unarchivedRecordList") List<SalaryRecord> records) {

        SalaryRecordRepository salaryRecordRepository = mock(SalaryRecordRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        when(salaryRecordRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryServiceImpl service = new SalaryServiceImpl(
                salaryRecordRepository,
                mock(UserRepository.class),
                mock(PointsService.class),
                auditLogRepository,
                mock(SalaryConfigService.class)
        );

        service.archiveSalaryRecords(1L);

        // All records should share the same archivedAt timestamp (set in a single batch)
        if (!records.isEmpty()) {
            var firstArchivedAt = records.get(0).getArchivedAt();
            for (SalaryRecord record : records) {
                assertThat(record.getArchivedAt())
                        .as("All records should share the same archivedAt timestamp")
                        .isEqualTo(firstArchivedAt);
            }
        }
    }

    // ========================================================================
    // Providers - generate random unarchived salary records
    // ========================================================================

    @Provide
    Arbitrary<List<SalaryRecord>> unarchivedRecordList() {
        return unarchivedRecord().list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<SalaryRecord> unarchivedRecord() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),
                Arbitraries.integers().between(0, 100),
                Arbitraries.integers().between(0, 300),
                Arbitraries.integers().between(0, 300),
                Arbitraries.integers().between(0, 800),
                Arbitraries.integers().between(0, 400)
        ).as((userId, basePoints, bonusPoints, deductions, totalPoints, miniCoins) ->
                SalaryRecord.builder()
                        .userId(userId)
                        .basePoints(basePoints)
                        .bonusPoints(bonusPoints)
                        .deductions(deductions)
                        .totalPoints(totalPoints)
                        .miniCoins(miniCoins)
                        .salaryAmount(BigDecimal.valueOf(miniCoins))
                        .archived(false)
                        .archivedAt(null)
                        .build()
        );
    }
}
