package com.pollen.management.service;

import com.pollen.management.dto.BatchSaveResponse;
import com.pollen.management.dto.SalaryReportDTO;
import com.pollen.management.entity.AuditLog;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryServiceImplTest {

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsService pointsService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private SalaryServiceImpl salaryService;

    // --- calculateSalaries tests ---

    @Test
    void calculateSalaries_shouldCreateRecordsForFiveFormalMembers() {
        List<User> members = createFormalMembers(5);
        when(userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER))).thenReturn(members);

        // Each member has 100 points → 200 mini coins raw
        for (User m : members) {
            when(pointsService.getTotalPoints(m.getId())).thenReturn(100);
            when(pointsService.convertPointsToMiniCoins(100)).thenReturn(200);
        }
        when(salaryRecordRepository.save(any(SalaryRecord.class))).thenAnswer(inv -> {
            SalaryRecord r = inv.getArgument(0);
            r.setId(r.getUserId());
            return r;
        });

        List<SalaryRecord> result = salaryService.calculateSalaries();

        assertEquals(5, result.size());
        // Equal points → equal distribution: 2000 / 5 = 400 each
        for (SalaryRecord record : result) {
            assertEquals(400, record.getMiniCoins());
            assertEquals(100, record.getTotalPoints());
        }
        // Total should equal pool
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertEquals(2000, total);
    }

    @Test
    void calculateSalaries_shouldThrowWhenMemberCountNotFive() {
        List<User> members = createFormalMembers(3);
        when(userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER))).thenReturn(members);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.calculateSalaries());
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("正式成员数量不符"));
    }

    @Test
    void calculateSalaries_shouldDistributeProportionally() {
        List<User> members = createFormalMembers(5);
        when(userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER))).thenReturn(members);

        // Member 1 has 200 points, others have 100 each → total raw = 400+200+200+200+200 = 1200 mini coins
        when(pointsService.getTotalPoints(1L)).thenReturn(200);
        when(pointsService.convertPointsToMiniCoins(200)).thenReturn(400);
        for (long i = 2; i <= 5; i++) {
            when(pointsService.getTotalPoints(i)).thenReturn(100);
            when(pointsService.convertPointsToMiniCoins(100)).thenReturn(200);
        }
        when(salaryRecordRepository.save(any(SalaryRecord.class))).thenAnswer(inv -> {
            SalaryRecord r = inv.getArgument(0);
            r.setId(r.getUserId());
            return r;
        });

        List<SalaryRecord> result = salaryService.calculateSalaries();

        assertEquals(5, result.size());
        // Total must equal pool (performance adjust redistributes within pool)
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertEquals(2000, total);
        // All should be within [200, 400] range after performance adjustment
        for (SalaryRecord record : result) {
            assertTrue(record.getMiniCoins() >= 200 && record.getMiniCoins() <= 400,
                    "Mini coins " + record.getMiniCoins() + " out of [200, 400] range");
        }
    }

    @Test
    void calculateSalaries_shouldHandleAllZeroPoints() {
        List<User> members = createFormalMembers(5);
        when(userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER))).thenReturn(members);

        for (User m : members) {
            when(pointsService.getTotalPoints(m.getId())).thenReturn(0);
            when(pointsService.convertPointsToMiniCoins(0)).thenReturn(0);
        }
        when(salaryRecordRepository.save(any(SalaryRecord.class))).thenAnswer(inv -> {
            SalaryRecord r = inv.getArgument(0);
            r.setId(r.getUserId());
            return r;
        });

        List<SalaryRecord> result = salaryService.calculateSalaries();

        assertEquals(5, result.size());
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertEquals(2000, total);
        // Equal distribution when all zero
        for (SalaryRecord record : result) {
            assertEquals(400, record.getMiniCoins());
        }
    }

    // --- getSalaryList tests ---

    @Test
    void getSalaryList_shouldReturnAllRecords() {
        List<SalaryRecord> expected = List.of(
                SalaryRecord.builder().id(1L).userId(1L).miniCoins(400).build(),
                SalaryRecord.builder().id(2L).userId(2L).miniCoins(300).build()
        );
        when(salaryRecordRepository.findAll()).thenReturn(expected);

        List<SalaryRecord> result = salaryService.getSalaryList();

        assertEquals(2, result.size());
        assertEquals(expected, result);
    }

    // --- updateSalaryRecord tests ---

    @Test
    void updateSalaryRecord_shouldUpdateFields() {
        SalaryRecord existing = SalaryRecord.builder()
                .id(1L).userId(1L).basePoints(100).bonusPoints(10)
                .deductions(5).totalPoints(105).miniCoins(210)
                .salaryAmount(new BigDecimal("210")).remark("old").build();
        when(salaryRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(salaryRecordRepository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simulate cell edit: update bonusPoints to 20 and remark
        SalaryRecord updates = SalaryRecord.builder()
                .basePoints(100)  // keep existing
                .bonusPoints(20)  // changed
                .deductions(5)    // keep existing
                .totalPoints(105) // keep existing
                .miniCoins(210)   // keep existing
                .salaryAmount(new BigDecimal("210"))
                .remark("updated")
                .build();

        SalaryRecord result = salaryService.updateSalaryRecord(1L, updates);

        assertEquals(20, result.getBonusPoints());
        assertEquals("updated", result.getRemark());
        assertEquals(100, result.getBasePoints());
        assertEquals(5, result.getDeductions());
    }

    @Test
    void updateSalaryRecord_shouldThrowWhenNotFound() {
        when(salaryRecordRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.updateSalaryRecord(99L, new SalaryRecord()));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("薪资记录不存在"));
    }

    // --- batchSave tests ---

    @Test
    void batchSave_shouldSaveValidRecords() {
        List<SalaryRecord> records = createValidBatchRecords();
        when(salaryRecordRepository.saveAll(records)).thenReturn(records);

        List<SalaryRecord> result = salaryService.batchSave(records);

        assertEquals(5, result.size());
        verify(salaryRecordRepository).saveAll(records);
    }

    @Test
    void batchSave_shouldRejectWhenMemberCountNotFive() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().userId(1L).miniCoins(300).build(),
                SalaryRecord.builder().userId(2L).miniCoins(300).build()
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.batchSave(records));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("正式成员数量不符"));
    }

    @Test
    void batchSave_shouldRejectWhenMiniCoinsBelowRange() {
        List<SalaryRecord> records = new ArrayList<>();
        records.add(SalaryRecord.builder().userId(1L).miniCoins(199).build()); // below 200
        for (long i = 2; i <= 5; i++) {
            records.add(SalaryRecord.builder().userId(i).miniCoins(400).build());
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.batchSave(records));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不在 [200, 400] 范围内"));
    }

    @Test
    void batchSave_shouldRejectWhenMiniCoinsAboveRange() {
        List<SalaryRecord> records = new ArrayList<>();
        records.add(SalaryRecord.builder().userId(1L).miniCoins(401).build()); // above 400
        for (long i = 2; i <= 5; i++) {
            records.add(SalaryRecord.builder().userId(i).miniCoins(300).build());
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.batchSave(records));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不在 [200, 400] 范围内"));
    }

    @Test
    void batchSave_shouldRejectWhenTotalExceedsPool() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder().userId(i).miniCoins(400).build()); // 5 * 400 = 2000, ok
        }
        // This is exactly 2000, should pass. Let's make it exceed:
        records.get(0).setMiniCoins(401); // now total = 2001

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.batchSave(records));
        assertEquals(400, ex.getCode());
        // Either range or total error
        assertTrue(ex.getMessage().contains("不在 [200, 400] 范围内") || ex.getMessage().contains("超过薪资池上限"));
    }

    @Test
    void batchSave_shouldAcceptExactPoolTotal() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder().userId(i).miniCoins(400).build()); // 5 * 400 = 2000
        }
        when(salaryRecordRepository.saveAll(records)).thenReturn(records);

        List<SalaryRecord> result = salaryService.batchSave(records);

        assertEquals(5, result.size());
    }

    // --- adjustToPool tests ---

    @Test
    void adjustToPool_shouldDistributeEquallyWhenAllZero() {
        List<Integer> raw = List.of(0, 0, 0, 0, 0);
        List<Integer> result = salaryService.adjustToPool(raw, 0);

        assertEquals(5, result.size());
        assertEquals(2000, result.stream().mapToInt(Integer::intValue).sum());
        for (int v : result) {
            assertEquals(400, v);
        }
    }

    @Test
    void adjustToPool_shouldDistributeProportionally() {
        // 400, 200, 200, 200, 200 → total 1200
        List<Integer> raw = List.of(400, 200, 200, 200, 200);
        List<Integer> result = salaryService.adjustToPool(raw, 1200);

        assertEquals(5, result.size());
        assertEquals(2000, result.stream().mapToInt(Integer::intValue).sum());
        // First member should get ~666, others ~333
        assertTrue(result.get(0) > result.get(1));
    }

    // --- performanceAdjust tests ---

    @Test
    void performanceAdjust_shouldCapAt400() {
        List<Integer> input = List.of(600, 500, 400, 300, 200);
        List<Integer> result = salaryService.performanceAdjust(input);

        for (int v : result) {
            assertTrue(v <= 400, "Value " + v + " exceeds 400");
        }
    }

    @Test
    void performanceAdjust_shouldNotChangeAlreadyInRange() {
        List<Integer> input = List.of(400, 400, 400, 400, 400);
        List<Integer> result = salaryService.performanceAdjust(input);

        assertEquals(input, result);
    }

    @Test
    void performanceAdjust_shouldRedistributeSurplus() {
        // One person has 600 (surplus 200), one has 100 (deficit 100)
        List<Integer> input = List.of(600, 100, 400, 400, 400);
        List<Integer> result = salaryService.performanceAdjust(input);

        assertEquals(400, result.get(0)); // capped
        assertTrue(result.get(1) >= 100); // should get some surplus
    }

    // --- validateBatch tests ---

    @Test
    void validateBatch_shouldPassForValidRecords() {
        List<SalaryRecord> records = createValidBatchRecords();
        // Should not throw
        assertDoesNotThrow(() -> salaryService.validateBatch(records));
    }

    @Test
    void validateBatch_shouldRejectEmptyList() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.validateBatch(List.of()));
        assertEquals(400, ex.getCode());
    }

    // --- batchSaveWithValidation tests ---

    @Test
    void batchSaveWithValidation_shouldSaveValidRecordsAndCreateAuditLog() {
        List<SalaryRecord> records = createValidBatchRecords();
        when(salaryRecordRepository.saveAll(records)).thenReturn(records);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertTrue(response.isSuccess());
        assertEquals(5, response.getSavedRecords().size());
        assertNull(response.getGlobalError());
        assertTrue(response.getErrors().isEmpty());
        verify(salaryRecordRepository).saveAll(records);
        verify(auditLogRepository).save(argThat(log ->
                log.getOperatorId().equals(100L) &&
                log.getOperationType().equals("SALARY_BATCH_SAVE")
        ));
    }

    @Test
    void batchSaveWithValidation_shouldReturnErrorsWhenMemberCountNotFive() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().userId(1L).miniCoins(300).build(),
                SalaryRecord.builder().userId(2L).miniCoins(300).build()
        );

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertFalse(response.isSuccess());
        assertNotNull(response.getGlobalError());
        assertTrue(response.getGlobalError().contains("正式成员数量不符"));
        verify(salaryRecordRepository, never()).saveAll(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void batchSaveWithValidation_shouldReturnViolatingUserIdsWhenMiniCoinsOutOfRange() {
        List<SalaryRecord> records = new ArrayList<>();
        records.add(SalaryRecord.builder().userId(1L).miniCoins(199).build()); // below 200
        records.add(SalaryRecord.builder().userId(2L).miniCoins(401).build()); // above 400
        records.add(SalaryRecord.builder().userId(3L).miniCoins(300).build());
        records.add(SalaryRecord.builder().userId(4L).miniCoins(300).build());
        records.add(SalaryRecord.builder().userId(5L).miniCoins(300).build());

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertFalse(response.isSuccess());
        assertEquals(2, response.getErrors().size());
        assertTrue(response.getViolatingUserIds().contains(1L));
        assertTrue(response.getViolatingUserIds().contains(2L));
        assertFalse(response.getViolatingUserIds().contains(3L));
        // Each error should reference the correct field
        for (BatchSaveResponse.ValidationError error : response.getErrors()) {
            assertEquals("miniCoins", error.getField());
        }
        verify(salaryRecordRepository, never()).saveAll(any());
    }

    @Test
    void batchSaveWithValidation_shouldReturnGlobalErrorWhenTotalExceedsPool() {
        List<SalaryRecord> records = new ArrayList<>();
        // 5 * 400 = 2000, but we set one to 400 and rest to 401 won't work (range check first)
        // Use valid range but total > 2000: 400 * 4 + 400 = 2000 is ok
        // Let's use 400 + 400 + 400 + 400 + 401 → range error first
        // Better: 400 + 400 + 400 + 400 + 400 = 2000 is ok
        // To test total only: 400 + 400 + 400 + 400 + 400 = 2000 (ok)
        // We need total > 2000 with all in [200,400]. Not possible since 5*400=2000.
        // So total > 2000 can only happen with range violations too.
        // Let's test with range violation that also causes total > 2000:
        records.add(SalaryRecord.builder().userId(1L).miniCoins(401).build());
        records.add(SalaryRecord.builder().userId(2L).miniCoins(400).build());
        records.add(SalaryRecord.builder().userId(3L).miniCoins(400).build());
        records.add(SalaryRecord.builder().userId(4L).miniCoins(400).build());
        records.add(SalaryRecord.builder().userId(5L).miniCoins(400).build());

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertFalse(response.isSuccess());
        // Should have range error for userId=1 AND global total error
        assertNotNull(response.getGlobalError());
        assertTrue(response.getGlobalError().contains("超过薪资池上限"));
        assertTrue(response.getViolatingUserIds().contains(1L));
    }

    @Test
    void batchSaveWithValidation_shouldHandleOptimisticLockConflict() {
        List<SalaryRecord> records = createValidBatchRecords();
        when(salaryRecordRepository.saveAll(records))
                .thenThrow(new ObjectOptimisticLockingFailureException(SalaryRecord.class.getName(), 1L));

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertFalse(response.isSuccess());
        assertNotNull(response.getGlobalError());
        assertTrue(response.getGlobalError().contains("并发修改冲突"));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void batchSaveWithValidation_shouldAcceptExactPoolTotal() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder().userId(i).miniCoins(400).build());
        }
        when(salaryRecordRepository.saveAll(records)).thenReturn(records);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertTrue(response.isSuccess());
        assertEquals(5, response.getSavedRecords().size());
    }

    @Test
    void batchSaveWithValidation_shouldAcceptMinimumValidCoins() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder().userId(i).miniCoins(200).build());
        }
        when(salaryRecordRepository.saveAll(records)).thenReturn(records);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchSaveResponse response = salaryService.batchSaveWithValidation(records, 100L);

        assertTrue(response.isSuccess());
    }

    // --- validateBatchDetailed tests ---

    @Test
    void validateBatchDetailed_shouldReturnSuccessForValidRecords() {
        List<SalaryRecord> records = createValidBatchRecords();
        BatchSaveResponse result = salaryService.validateBatchDetailed(records);
        assertTrue(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateBatchDetailed_shouldCollectAllRangeViolations() {
        List<SalaryRecord> records = new ArrayList<>();
        records.add(SalaryRecord.builder().userId(10L).miniCoins(100).build());
        records.add(SalaryRecord.builder().userId(20L).miniCoins(500).build());
        records.add(SalaryRecord.builder().userId(30L).miniCoins(300).build());
        records.add(SalaryRecord.builder().userId(40L).miniCoins(300).build());
        records.add(SalaryRecord.builder().userId(50L).miniCoins(300).build());

        BatchSaveResponse result = salaryService.validateBatchDetailed(records);

        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrors().size());
        assertEquals(2, result.getViolatingUserIds().size());
        assertTrue(result.getViolatingUserIds().contains(10L));
        assertTrue(result.getViolatingUserIds().contains(20L));
    }

    // --- generateSalaryReport tests ---

    @Test
    void generateSalaryReport_shouldReturnReportWithAllUnarchivedRecords() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            records.add(SalaryRecord.builder()
                    .id(i).userId(i).basePoints(100).bonusPoints(10)
                    .deductions(5).totalPoints(105).miniCoins(400)
                    .salaryAmount(new BigDecimal("400")).remark("test").archived(false).build());
        }
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        for (long i = 1; i <= 3; i++) {
            User user = User.builder().id(i).username("member" + i).role(Role.MEMBER).build();
            when(userRepository.findById(i)).thenReturn(Optional.of(user));
        }

        SalaryReportDTO report = salaryService.generateSalaryReport();

        assertNotNull(report);
        assertNotNull(report.getGeneratedAt());
        assertEquals(2000, report.getSalaryPoolTotal());
        assertEquals(1200, report.getAllocatedTotal());
        assertEquals(3, report.getDetails().size());

        SalaryReportDTO.MemberSalaryDetail detail = report.getDetails().get(0);
        assertEquals(1L, detail.getUserId());
        assertEquals("member1", detail.getUsername());
        assertEquals("MEMBER", detail.getRole());
        assertEquals(100, detail.getBasePoints());
        assertEquals(10, detail.getBonusPoints());
        assertEquals(5, detail.getDeductions());
        assertEquals(105, detail.getTotalPoints());
        assertEquals(400, detail.getMiniCoins());
        assertEquals("test", detail.getRemark());
    }

    @Test
    void generateSalaryReport_shouldThrowWhenNoUnarchivedRecords() {
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.generateSalaryReport());
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("没有未归档的薪资记录"));
    }

    @Test
    void generateSalaryReport_shouldHandleUnknownUser() {
        List<SalaryRecord> records = List.of(
                SalaryRecord.builder().id(1L).userId(999L).basePoints(50).bonusPoints(0)
                        .deductions(0).totalPoints(50).miniCoins(300)
                        .salaryAmount(new BigDecimal("300")).archived(false).build()
        );
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        SalaryReportDTO report = salaryService.generateSalaryReport();

        assertEquals(1, report.getDetails().size());
        assertEquals("unknown", report.getDetails().get(0).getUsername());
        assertEquals("unknown", report.getDetails().get(0).getRole());
    }

    // --- archiveSalaryRecords tests ---

    @Test
    void archiveSalaryRecords_shouldArchiveAllUnarchivedRecords() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder()
                    .id(i).userId(i).miniCoins(400).archived(false).build());
        }
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        when(salaryRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = salaryService.archiveSalaryRecords(100L);

        assertEquals(5, count);
        for (SalaryRecord record : records) {
            assertTrue(record.getArchived());
            assertNotNull(record.getArchivedAt());
        }
        verify(salaryRecordRepository).saveAll(records);
        verify(auditLogRepository).save(argThat(log ->
                log.getOperatorId().equals(100L) &&
                log.getOperationType().equals("SALARY_ARCHIVE") &&
                log.getOperationDetail().contains("归档薪资记录 5 条")
        ));
    }

    @Test
    void archiveSalaryRecords_shouldReturnZeroWhenNoRecordsToArchive() {
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(List.of());

        int count = salaryService.archiveSalaryRecords(100L);

        assertEquals(0, count);
        verify(salaryRecordRepository, never()).saveAll(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void archiveSalaryRecords_shouldSetSameArchivedAtForAllRecords() {
        List<SalaryRecord> records = new ArrayList<>();
        records.add(SalaryRecord.builder().id(1L).userId(1L).miniCoins(400).archived(false).build());
        records.add(SalaryRecord.builder().id(2L).userId(2L).miniCoins(400).archived(false).build());
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        when(salaryRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        salaryService.archiveSalaryRecords(100L);

        // All records should have the same archivedAt timestamp
        assertEquals(records.get(0).getArchivedAt(), records.get(1).getArchivedAt());
    }

    // --- Helper methods ---

    private List<User> createFormalMembers(int count) {
        List<User> members = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            User user = User.builder()
                    .id((long) i)
                    .username("member" + i)
                    .password("pass")
                    .role(i == 1 ? Role.VICE_LEADER : Role.MEMBER)
                    .enabled(true)
                    .build();
            members.add(user);
        }
        return members;
    }

    private List<SalaryRecord> createValidBatchRecords() {
        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder()
                    .userId(i)
                    .miniCoins(400)
                    .build());
        }
        return records;
    }
}
