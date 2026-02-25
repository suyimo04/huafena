package com.pollen.management.service;

import com.pollen.management.dto.BatchSaveResponse;
import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.SalaryCalculationResult;
import com.pollen.management.dto.SalaryDimensionInput;
import com.pollen.management.dto.SalaryMemberDTO;
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

    @Mock
    private SalaryConfigService salaryConfigService;

    @InjectMocks
    private SalaryServiceImpl salaryService;

    // --- calculateSalaries tests ---

    @Test
    void calculateSalaries_shouldCreateRecordsForFiveFormalMembers() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
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
        // Total raw 1000 <= pool 2000, so original values kept (Requirement 4.3)
        // All values already in [200, 400], so performanceAdjust keeps them as-is
        for (SalaryRecord record : result) {
            assertEquals(200, record.getMiniCoins());
            assertEquals(100, record.getTotalPoints());
        }
        // Total should equal sum of original values
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertEquals(1000, total);
    }

    @Test
    void calculateSalaries_shouldThrowWhenMemberCountNotFive() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        List<User> members = createFormalMembers(3);
        when(userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER))).thenReturn(members);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.calculateSalaries());
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("正式成员数量不符"));
    }

    @Test
    void calculateSalaries_shouldDistributeProportionally() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
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
        // Total raw 1200 <= pool 2000, so original values are kept (Requirement 4.3)
        // All values already in [200, 400], so performanceAdjust keeps them as-is
        assertEquals(400, result.get(0).getMiniCoins()); // member 1
        for (int i = 1; i < 5; i++) {
            assertEquals(200, result.get(i).getMiniCoins()); // members 2-5
        }
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertEquals(1200, total);
    }

    @Test
    void calculateSalaries_shouldScaleDownWhenExceedsPool() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        List<User> members = createFormalMembers(5);
        when(userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER))).thenReturn(members);

        // Each member has 300 points → 600 mini coins raw → total 3000 > pool 2000
        for (User m : members) {
            when(pointsService.getTotalPoints(m.getId())).thenReturn(300);
            when(pointsService.convertPointsToMiniCoins(300)).thenReturn(600);
        }
        when(salaryRecordRepository.save(any(SalaryRecord.class))).thenAnswer(inv -> {
            SalaryRecord r = inv.getArgument(0);
            r.setId(r.getUserId());
            return r;
        });

        List<SalaryRecord> result = salaryService.calculateSalaries();

        assertEquals(5, result.size());
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertTrue(total <= 2000, "Total " + total + " should not exceed pool 2000");
        for (SalaryRecord record : result) {
            assertTrue(record.getMiniCoins() >= 200 && record.getMiniCoins() <= 400,
                    "Mini coins " + record.getMiniCoins() + " out of [200, 400] range");
        }
    }

    @Test
    void calculateSalaries_shouldHandleAllZeroPoints() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        List<SalaryRecord> records = createValidBatchRecords();
        when(salaryRecordRepository.saveAll(records)).thenReturn(records);

        List<SalaryRecord> result = salaryService.batchSave(records);

        assertEquals(5, result.size());
        verify(salaryRecordRepository).saveAll(records);
    }

    @Test
    void batchSave_shouldRejectWhenMemberCountNotFive() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        List<Integer> raw = List.of(0, 0, 0, 0, 0);
        List<Integer> result = salaryService.adjustToPool(raw, 0);

        assertEquals(5, result.size());
        assertEquals(2000, result.stream().mapToInt(Integer::intValue).sum());
        for (int v : result) {
            assertEquals(400, v);
        }
    }

    @Test
    void adjustToPool_shouldScaleProportionallyWhenExceedsPool() {
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        // total 3000 > pool 2000, should scale down proportionally
        List<Integer> raw = List.of(600, 600, 600, 600, 600);
        List<Integer> result = salaryService.adjustToPool(raw, 3000);

        assertEquals(5, result.size());
        assertEquals(2000, result.stream().mapToInt(Integer::intValue).sum());
        // Each should get ~400
        for (int v : result) {
            assertEquals(400, v);
        }
    }

    @Test
    void adjustToPool_shouldKeepOriginalWhenBelowPool() {
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        // total 1200 <= pool 2000, should keep original values (Requirement 4.3)
        List<Integer> raw = List.of(400, 200, 200, 200, 200);
        List<Integer> result = salaryService.adjustToPool(raw, 1200);

        assertEquals(5, result.size());
        assertEquals(List.of(400, 200, 200, 200, 200), result);
    }

    @Test
    void adjustToPool_shouldUseConfiguredPoolTotal() {
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(3000);
        // total 6000 > pool 3000, should scale down proportionally
        List<Integer> raw = List.of(1200, 1200, 1200, 1200, 1200);
        List<Integer> result = salaryService.adjustToPool(raw, 6000);

        assertEquals(5, result.size());
        assertEquals(3000, result.stream().mapToInt(Integer::intValue).sum());
    }

    // --- performanceAdjust tests ---

    @Test
    void performanceAdjust_shouldCapAtMax() {
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        List<Integer> input = List.of(600, 500, 400, 300, 200);
        List<Integer> result = salaryService.performanceAdjust(input);

        for (int v : result) {
            assertTrue(v <= 400, "Value " + v + " exceeds 400");
            assertTrue(v >= 200, "Value " + v + " below 200");
        }
    }

    @Test
    void performanceAdjust_shouldNotChangeAlreadyInRange() {
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        List<Integer> input = List.of(400, 400, 400, 400, 400);
        List<Integer> result = salaryService.performanceAdjust(input);

        assertEquals(input, result);
    }

    @Test
    void performanceAdjust_shouldRedistributeSurplus() {
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        // One person has 600 (surplus 200), one has 100 (deficit 100)
        List<Integer> input = List.of(600, 100, 400, 400, 400);
        List<Integer> result = salaryService.performanceAdjust(input);

        assertEquals(400, result.get(0)); // capped at max
        assertTrue(result.get(1) >= 200); // raised to at least min
    }

    @Test
    void performanceAdjust_shouldUseConfiguredRange() {
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{100, 500});
        List<Integer> input = List.of(700, 50, 300, 300, 300);
        List<Integer> result = salaryService.performanceAdjust(input);

        for (int v : result) {
            assertTrue(v <= 500, "Value " + v + " exceeds configured max 500");
            assertTrue(v >= 100, "Value " + v + " below configured min 100");
        }
    }

    // --- validateBatch tests ---

    @Test
    void validateBatch_shouldPassForValidRecords() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        List<SalaryRecord> records = createValidBatchRecords();
        // Should not throw
        assertDoesNotThrow(() -> salaryService.validateBatch(records));
    }

    @Test
    void validateBatch_shouldRejectEmptyList() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.validateBatch(List.of()));
        assertEquals(400, ex.getCode());
    }

    // --- batchSaveWithValidation tests ---

    @Test
    void batchSaveWithValidation_shouldSaveValidRecordsAndCreateAuditLog() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        List<SalaryRecord> records = createValidBatchRecords();
        BatchSaveResponse result = salaryService.validateBatchDetailed(records);
        assertTrue(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateBatchDetailed_shouldCollectAllRangeViolations() {
        when(salaryConfigService.getFormalMemberCount()).thenReturn(5);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
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

    // --- calculateMemberPoints tests ---

    @Test
    void calculateMemberPoints_shouldComputeBaseAndBonusPointsCorrectly() {
        setupDefaultCheckinTiersAndRatio();

        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(80)
                .checkinCount(45)  // 良好 → +30
                .violationHandlingCount(2)  // 2 × 3 = 6
                .taskCompletionPoints(15)
                .announcementCount(3)  // 3 × 5 = 15
                .eventHostingPoints(20)
                .birthdayBonusPoints(25)
                .monthlyExcellentPoints(10)
                .build();

        SalaryCalculationResult result = salaryService.calculateMemberPoints(input);

        // base = 80 + 30 + 6 + 15 + 15 = 146
        assertEquals(146, result.getBasePoints());
        // bonus = 20 + 25 + 10 = 55
        assertEquals(55, result.getBonusPoints());
        // total = 146 + 55 = 201
        assertEquals(201, result.getTotalPoints());
        // miniCoins = 201 × 2 = 402
        assertEquals(402, result.getMiniCoins());
        assertEquals(30, result.getCheckinPoints());
        assertEquals(6, result.getViolationHandlingPoints());
        assertEquals(15, result.getAnnouncementPoints());
        assertEquals("良好", result.getCheckinLevel());
    }

    @Test
    void calculateMemberPoints_shouldHandleZeroInputs() {
        setupDefaultCheckinTiersAndRatio();

        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(0)
                .checkinCount(0)  // 不合格 → -20
                .violationHandlingCount(0)
                .taskCompletionPoints(0)
                .announcementCount(0)
                .eventHostingPoints(0)
                .birthdayBonusPoints(0)
                .monthlyExcellentPoints(0)
                .build();

        SalaryCalculationResult result = salaryService.calculateMemberPoints(input);

        // base = 0 + (-20) + 0 + 0 + 0 = -20
        assertEquals(-20, result.getBasePoints());
        assertEquals(0, result.getBonusPoints());
        assertEquals(-20, result.getTotalPoints());
        assertEquals(-40, result.getMiniCoins());
        assertEquals(-20, result.getCheckinPoints());
        assertEquals("不合格", result.getCheckinLevel());
    }

    @Test
    void calculateMemberPoints_shouldHandleNegativeCheckinCountAsZero() {
        setupDefaultCheckinTiersAndRatio();

        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(50)
                .checkinCount(-5)  // negative → treated as 0 → 不合格 → -20
                .violationHandlingCount(0)
                .taskCompletionPoints(10)
                .announcementCount(0)
                .eventHostingPoints(0)
                .birthdayBonusPoints(0)
                .monthlyExcellentPoints(0)
                .build();

        SalaryCalculationResult result = salaryService.calculateMemberPoints(input);

        assertEquals(-20, result.getCheckinPoints());
        assertEquals("不合格", result.getCheckinLevel());
        // base = 50 + (-20) + 0 + 10 + 0 = 40
        assertEquals(40, result.getBasePoints());
    }

    @Test
    void calculateMemberPoints_checkinTierBoundaries() {
        setupDefaultCheckinTiersAndRatio();

        // Test each tier boundary
        assertCheckinPoints(0, -20, "不合格");
        assertCheckinPoints(19, -20, "不合格");
        assertCheckinPoints(20, -10, "需改进");
        assertCheckinPoints(29, -10, "需改进");
        assertCheckinPoints(30, 0, "合格");
        assertCheckinPoints(39, 0, "合格");
        assertCheckinPoints(40, 30, "良好");
        assertCheckinPoints(49, 30, "良好");
        assertCheckinPoints(50, 50, "优秀");
        assertCheckinPoints(999, 50, "优秀");
    }

    @Test
    void calculateMemberPoints_shouldRejectCommunityActivityAbove100() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(101)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("社群活跃度积分超出范围"));
    }

    @Test
    void calculateMemberPoints_shouldRejectNegativeCommunityActivity() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(-1)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("社群活跃度积分超出范围"));
    }

    @Test
    void calculateMemberPoints_shouldRejectTaskCompletionAbove100() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(50)
                .taskCompletionPoints(101)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("任务完成积分超出范围"));
    }

    @Test
    void calculateMemberPoints_shouldRejectNegativeViolationCount() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(50)
                .violationHandlingCount(-1)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("违规处理次数不能为负数"));
    }

    @Test
    void calculateMemberPoints_shouldRejectNegativeAnnouncementCount() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(50)
                .announcementCount(-1)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("公告发布次数不能为负数"));
    }

    @Test
    void calculateMemberPoints_shouldRejectEventHostingAbove250() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(50)
                .eventHostingPoints(251)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("活动举办积分超出范围"));
    }

    @Test
    void calculateMemberPoints_shouldRejectBirthdayBonusAbove25() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(50)
                .birthdayBonusPoints(26)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("生日福利积分超出范围"));
    }

    @Test
    void calculateMemberPoints_shouldRejectMonthlyExcellentAbove30() {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .communityActivityPoints(50)
                .monthlyExcellentPoints(31)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> salaryService.calculateMemberPoints(input));
        assertTrue(ex.getMessage().contains("月度优秀评议积分超出范围"));
    }

    @Test
    void calculateMemberPoints_shouldAcceptBoundaryValues() {
        setupDefaultCheckinTiersAndRatio();

        // All at max valid values
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(100)
                .checkinCount(50)  // 优秀 → +50
                .violationHandlingCount(0)
                .taskCompletionPoints(100)
                .announcementCount(0)
                .eventHostingPoints(250)
                .birthdayBonusPoints(25)
                .monthlyExcellentPoints(30)
                .build();

        SalaryCalculationResult result = salaryService.calculateMemberPoints(input);

        // base = 100 + 50 + 0 + 100 + 0 = 250
        assertEquals(250, result.getBasePoints());
        // bonus = 250 + 25 + 30 = 305
        assertEquals(305, result.getBonusPoints());
        assertEquals(555, result.getTotalPoints());
        assertEquals(1110, result.getMiniCoins());
    }

    // --- lookupCheckinTier tests ---

    @Test
    void lookupCheckinTier_shouldReturnZeroForEmptyTiers() {
        int result = salaryService.lookupCheckinTier(25, List.of());
        assertEquals(0, result);
    }

    @Test
    void lookupCheckinTier_shouldReturnZeroWhenNoTierMatches() {
        List<CheckinTier> tiers = List.of(
                CheckinTier.builder().minCount(10).maxCount(20).points(5).label("test").build()
        );
        int result = salaryService.lookupCheckinTier(25, tiers);
        assertEquals(0, result);
    }

    @Test
    void lookupCheckinTier_shouldHandleNegativeCountAsZero() {
        List<CheckinTier> tiers = getDefaultCheckinTiers();
        int result = salaryService.lookupCheckinTier(-10, tiers);
        assertEquals(-20, result); // 0 falls in [0, 19] → -20
    }

    // --- calculateAndDistribute tests ---

    @Test
    void calculateAndDistribute_shouldComputeDimensionsAndDistribute() {
        when(salaryConfigService.getCheckinTiers()).thenReturn(getDefaultCheckinTiers());
        when(salaryConfigService.getPointsToCoinsRatio()).thenReturn(2);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});

        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            records.add(SalaryRecord.builder()
                    .id(i).userId(i)
                    .communityActivityPoints(50)
                    .checkinCount(35)  // 合格 → 0
                    .violationHandlingCount(1)  // 1 × 3 = 3
                    .taskCompletionPoints(10)
                    .announcementCount(2)  // 2 × 5 = 10
                    .eventHostingPoints(15)
                    .birthdayBonusPoints(0)
                    .monthlyExcellentPoints(10)
                    .archived(false)
                    .build());
        }
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        when(salaryRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<SalaryRecord> result = salaryService.calculateAndDistribute();

        assertEquals(3, result.size());
        for (SalaryRecord r : result) {
            // base = 50 + 0 + 3 + 10 + 10 = 73
            assertEquals(73, r.getBasePoints());
            // bonus = 15 + 0 + 10 = 25
            assertEquals(25, r.getBonusPoints());
            // total = 73 + 25 = 98
            assertEquals(98, r.getTotalPoints());
            // checkinPoints = 0 (合格)
            assertEquals(0, r.getCheckinPoints());
            // violationHandlingPoints = 3
            assertEquals(3, r.getViolationHandlingPoints());
            // announcementPoints = 10
            assertEquals(10, r.getAnnouncementPoints());
        }
        // raw miniCoins = 98 × 2 = 196 each, total 588 <= 2000, kept as-is
        // performanceAdjust: 196 < 200 → raised to 200
        for (SalaryRecord r : result) {
            assertTrue(r.getMiniCoins() >= 200);
        }
        verify(salaryRecordRepository).saveAll(anyList());
    }

    @Test
    void calculateAndDistribute_shouldThrowWhenNoRecords() {
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> salaryService.calculateAndDistribute());
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("没有未归档的薪资记录"));
    }

    @Test
    void calculateAndDistribute_shouldScaleDownWhenExceedsPool() {
        when(salaryConfigService.getCheckinTiers()).thenReturn(getDefaultCheckinTiers());
        when(salaryConfigService.getPointsToCoinsRatio()).thenReturn(2);
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        when(salaryConfigService.getMiniCoinsRange()).thenReturn(new int[]{200, 400});

        List<SalaryRecord> records = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            records.add(SalaryRecord.builder()
                    .id(i).userId(i)
                    .communityActivityPoints(100)
                    .checkinCount(50)  // 优秀 → +50
                    .violationHandlingCount(5)  // 5 × 3 = 15
                    .taskCompletionPoints(50)
                    .announcementCount(10)  // 10 × 5 = 50
                    .eventHostingPoints(100)
                    .birthdayBonusPoints(25)
                    .monthlyExcellentPoints(30)
                    .archived(false)
                    .build());
        }
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(records);
        when(salaryRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<SalaryRecord> result = salaryService.calculateAndDistribute();

        assertEquals(5, result.size());
        int total = result.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        assertTrue(total <= 2000, "Total " + total + " should not exceed pool 2000");
        for (SalaryRecord r : result) {
            assertTrue(r.getMiniCoins() >= 200 && r.getMiniCoins() <= 400,
                    "Mini coins " + r.getMiniCoins() + " out of [200, 400] range");
        }
    }

    // --- getSalaryMembers dimension fields tests ---

    @Test
    void getSalaryMembers_shouldReturnDimensionDetailFields() {
        User member = User.builder().id(1L).username("member1").role(Role.VICE_LEADER).enabled(true).build();
        when(userRepository.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN)))
                .thenReturn(List.of(member));

        SalaryRecord record = SalaryRecord.builder()
                .id(10L).userId(1L)
                .basePoints(146).bonusPoints(55).deductions(0).totalPoints(201).miniCoins(400)
                .salaryAmount(BigDecimal.valueOf(400))
                .communityActivityPoints(80)
                .checkinCount(45).checkinPoints(30)
                .violationHandlingCount(2).violationHandlingPoints(6)
                .taskCompletionPoints(15)
                .announcementCount(3).announcementPoints(15)
                .eventHostingPoints(20)
                .birthdayBonusPoints(25)
                .monthlyExcellentPoints(10)
                .archived(false)
                .build();
        when(salaryRecordRepository.findAll()).thenReturn(List.of(record));

        List<SalaryMemberDTO> result = salaryService.getSalaryMembers();

        assertEquals(1, result.size());
        SalaryMemberDTO dto = result.get(0);
        assertEquals(80, dto.getCommunityActivityPoints());
        assertEquals(45, dto.getCheckinCount());
        assertEquals(30, dto.getCheckinPoints());
        assertEquals(2, dto.getViolationHandlingCount());
        assertEquals(6, dto.getViolationHandlingPoints());
        assertEquals(15, dto.getTaskCompletionPoints());
        assertEquals(3, dto.getAnnouncementCount());
        assertEquals(15, dto.getAnnouncementPoints());
        assertEquals(20, dto.getEventHostingPoints());
        assertEquals(25, dto.getBirthdayBonusPoints());
        assertEquals(10, dto.getMonthlyExcellentPoints());
    }

    @Test
    void getSalaryMembers_shouldReturnZeroDimensionFieldsWhenNoRecord() {
        User member = User.builder().id(1L).username("member1").role(Role.LEADER).enabled(true).build();
        when(userRepository.findByRoleIn(List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN)))
                .thenReturn(List.of(member));
        when(salaryRecordRepository.findAll()).thenReturn(List.of());

        List<SalaryMemberDTO> result = salaryService.getSalaryMembers();

        assertEquals(1, result.size());
        SalaryMemberDTO dto = result.get(0);
        assertEquals(0, dto.getCommunityActivityPoints());
        assertEquals(0, dto.getCheckinCount());
        assertEquals(0, dto.getCheckinPoints());
        assertEquals(0, dto.getViolationHandlingCount());
        assertEquals(0, dto.getViolationHandlingPoints());
        assertEquals(0, dto.getTaskCompletionPoints());
        assertEquals(0, dto.getAnnouncementCount());
        assertEquals(0, dto.getAnnouncementPoints());
        assertEquals(0, dto.getEventHostingPoints());
        assertEquals(0, dto.getBirthdayBonusPoints());
        assertEquals(0, dto.getMonthlyExcellentPoints());
    }

    // --- generateSalaryReport dimension fields tests ---

    @Test
    void generateSalaryReport_shouldIncludeDimensionDetailsAndRemainingAmount() {
        when(salaryConfigService.getSalaryPoolTotal()).thenReturn(2000);
        SalaryRecord record = SalaryRecord.builder()
                .id(1L).userId(1L)
                .basePoints(146).bonusPoints(55).deductions(0).totalPoints(201).miniCoins(400)
                .salaryAmount(BigDecimal.valueOf(400))
                .communityActivityPoints(80)
                .checkinCount(45).checkinPoints(30)
                .violationHandlingCount(2).violationHandlingPoints(6)
                .taskCompletionPoints(15)
                .announcementCount(3).announcementPoints(15)
                .eventHostingPoints(20)
                .birthdayBonusPoints(25)
                .monthlyExcellentPoints(10)
                .archived(false)
                .build();
        when(salaryRecordRepository.findByArchivedFalse()).thenReturn(List.of(record));
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).username("member1").role(Role.MEMBER).build()));

        SalaryReportDTO report = salaryService.generateSalaryReport();

        assertEquals(2000, report.getSalaryPoolTotal());
        assertEquals(400, report.getAllocatedTotal());
        assertEquals(1600, report.getRemainingAmount());

        SalaryReportDTO.MemberSalaryDetail detail = report.getDetails().get(0);
        assertEquals(80, detail.getCommunityActivityPoints());
        assertEquals(45, detail.getCheckinCount());
        assertEquals(30, detail.getCheckinPoints());
        assertEquals(2, detail.getViolationHandlingCount());
        assertEquals(6, detail.getViolationHandlingPoints());
        assertEquals(15, detail.getTaskCompletionPoints());
        assertEquals(3, detail.getAnnouncementCount());
        assertEquals(15, detail.getAnnouncementPoints());
        assertEquals(20, detail.getEventHostingPoints());
        assertEquals(25, detail.getBirthdayBonusPoints());
        assertEquals(10, detail.getMonthlyExcellentPoints());
    }

    // --- Helper methods ---

    private void setupDefaultCheckinTiersAndRatio() {
        when(salaryConfigService.getCheckinTiers()).thenReturn(getDefaultCheckinTiers());
        when(salaryConfigService.getPointsToCoinsRatio()).thenReturn(2);
    }

    private List<CheckinTier> getDefaultCheckinTiers() {
        return List.of(
                CheckinTier.builder().minCount(0).maxCount(19).points(-20).label("不合格").build(),
                CheckinTier.builder().minCount(20).maxCount(29).points(-10).label("需改进").build(),
                CheckinTier.builder().minCount(30).maxCount(39).points(0).label("合格").build(),
                CheckinTier.builder().minCount(40).maxCount(49).points(30).label("良好").build(),
                CheckinTier.builder().minCount(50).maxCount(999).points(50).label("优秀").build()
        );
    }

    private void assertCheckinPoints(int checkinCount, int expectedPoints, String expectedLevel) {
        SalaryDimensionInput input = SalaryDimensionInput.builder()
                .userId(1L)
                .communityActivityPoints(0)
                .checkinCount(checkinCount)
                .violationHandlingCount(0)
                .taskCompletionPoints(0)
                .announcementCount(0)
                .eventHostingPoints(0)
                .birthdayBonusPoints(0)
                .monthlyExcellentPoints(0)
                .build();

        SalaryCalculationResult result = salaryService.calculateMemberPoints(input);
        assertEquals(expectedPoints, result.getCheckinPoints(),
                "Checkin count " + checkinCount + " should yield " + expectedPoints + " points");
        assertEquals(expectedLevel, result.getCheckinLevel(),
                "Checkin count " + checkinCount + " should yield level " + expectedLevel);
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
