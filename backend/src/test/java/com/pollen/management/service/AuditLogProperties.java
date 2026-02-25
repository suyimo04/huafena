package com.pollen.management.service;

import com.pollen.management.entity.AuditLog;
import com.pollen.management.repository.AuditLogRepository;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: pollen-group-management, Property 25: 关键操作审计日志
 * **Validates: Requirements 10.2**
 *
 * Property 25: For any critical operation (薪资保存、角色变更、申请审核、面试复审),
 * after the operation completes, there should exist a corresponding audit log record
 * containing the operator, operation type, operation time, and operation details.
 */
class AuditLogProperties {

    private static final List<String> CRITICAL_OPERATION_TYPES = Arrays.asList(
            "薪资保存", "角色变更", "申请审核", "面试复审"
    );

    /**
     * For any valid operatorId, critical operation type, and detail string,
     * calling logOperation creates an audit log with all fields correctly set:
     * operatorId matches, operationType matches, operationTime is set (not null),
     * and operationDetail matches.
     */
    @Property(tries = 100)
    void criticalOperationCreatesAuditLogWithAllFields(
            @ForAll("operatorIds") Long operatorId,
            @ForAll("criticalOperationTypes") String operationType,
            @ForAll("operationDetails") String detail) {

        AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });

        AuditLogServiceImpl service = new AuditLogServiceImpl(repository);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        service.logOperation(operatorId, operationType, detail);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getOperatorId()).isEqualTo(operatorId);
        assertThat(saved.getOperationType()).isEqualTo(operationType);
        assertThat(saved.getOperationTime()).isNotNull();
        assertThat(saved.getOperationTime()).isAfterOrEqualTo(before);
        assertThat(saved.getOperationTime()).isBeforeOrEqualTo(after);
        assertThat(saved.getOperationDetail()).isEqualTo(detail);
    }

    // --- Arbitraries ---

    @Provide
    Arbitrary<Long> operatorIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> criticalOperationTypes() {
        return Arbitraries.of(CRITICAL_OPERATION_TYPES);
    }

    @Provide
    Arbitrary<String> operationDetails() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(200);
    }
}
