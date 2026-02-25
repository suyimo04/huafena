package com.pollen.management.service;

import com.pollen.management.entity.AuditLog;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    // --- logOperation tests ---

    @Test
    void logOperation_shouldSaveAuditLogWithCorrectFields() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });

        auditLogService.logOperation(10L, "薪资保存", "批量保存5条薪资记录");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(10L, saved.getOperatorId());
        assertEquals("薪资保存", saved.getOperationType());
        assertEquals("批量保存5条薪资记录", saved.getOperationDetail());
        assertNotNull(saved.getOperationTime());
    }

    @Test
    void logOperation_shouldSetOperationTimeToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.logOperation(1L, "角色变更", "用户角色从INTERN变更为MEMBER");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        LocalDateTime operationTime = captor.getValue().getOperationTime();
        assertTrue(operationTime.isAfter(before) && operationTime.isBefore(after));
    }

    @Test
    void logOperation_shouldAllowNullDetail() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.logOperation(1L, "申请审核", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getOperationDetail());
    }

    @Test
    void logOperation_shouldThrowWhenOperatorIdIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> auditLogService.logOperation(null, "薪资保存", "detail"));
        assertEquals(400, ex.getCode());
        assertEquals("操作人ID不能为空", ex.getMessage());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void logOperation_shouldThrowWhenOperationTypeIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> auditLogService.logOperation(1L, null, "detail"));
        assertEquals(400, ex.getCode());
        assertEquals("操作类型不能为空", ex.getMessage());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void logOperation_shouldThrowWhenOperationTypeIsBlank() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> auditLogService.logOperation(1L, "  ", "detail"));
        assertEquals(400, ex.getCode());
        assertEquals("操作类型不能为空", ex.getMessage());
        verify(auditLogRepository, never()).save(any());
    }

    // --- getAuditLogs tests ---

    @Test
    void getAuditLogs_shouldReturnAllLogsOrderedByTimeDesc() {
        AuditLog log1 = AuditLog.builder().id(1L).operatorId(1L).operationType("薪资保存")
                .operationTime(LocalDateTime.of(2025, 1, 1, 10, 0)).build();
        AuditLog log2 = AuditLog.builder().id(2L).operatorId(2L).operationType("角色变更")
                .operationTime(LocalDateTime.of(2025, 1, 2, 10, 0)).build();
        when(auditLogRepository.findAllByOrderByOperationTimeDesc()).thenReturn(List.of(log2, log1));

        List<AuditLog> result = auditLogService.getAuditLogs();

        assertEquals(2, result.size());
        assertEquals("角色变更", result.get(0).getOperationType());
        assertEquals("薪资保存", result.get(1).getOperationType());
    }

    @Test
    void getAuditLogs_shouldReturnEmptyListWhenNoLogs() {
        when(auditLogRepository.findAllByOrderByOperationTimeDesc()).thenReturn(List.of());

        List<AuditLog> result = auditLogService.getAuditLogs();

        assertTrue(result.isEmpty());
    }

    // --- getAuditLogsByType tests ---

    @Test
    void getAuditLogsByType_shouldReturnFilteredLogs() {
        AuditLog log1 = AuditLog.builder().id(1L).operatorId(1L).operationType("薪资保存")
                .operationTime(LocalDateTime.of(2025, 1, 1, 10, 0)).build();
        when(auditLogRepository.findByOperationTypeOrderByOperationTimeDesc("薪资保存"))
                .thenReturn(List.of(log1));

        List<AuditLog> result = auditLogService.getAuditLogsByType("薪资保存");

        assertEquals(1, result.size());
        assertEquals("薪资保存", result.get(0).getOperationType());
    }

    @Test
    void getAuditLogsByType_shouldReturnEmptyListWhenNoMatchingLogs() {
        when(auditLogRepository.findByOperationTypeOrderByOperationTimeDesc("不存在的类型"))
                .thenReturn(List.of());

        List<AuditLog> result = auditLogService.getAuditLogsByType("不存在的类型");

        assertTrue(result.isEmpty());
    }

    @Test
    void getAuditLogsByType_shouldThrowWhenTypeIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> auditLogService.getAuditLogsByType(null));
        assertEquals(400, ex.getCode());
        assertEquals("操作类型不能为空", ex.getMessage());
    }

    @Test
    void getAuditLogsByType_shouldThrowWhenTypeIsBlank() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> auditLogService.getAuditLogsByType("  "));
        assertEquals(400, ex.getCode());
        assertEquals("操作类型不能为空", ex.getMessage());
    }
}
