package com.pollen.management.service;

import com.pollen.management.entity.AuditLog;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logOperation(Long operatorId, String operationType, String detail) {
        if (operatorId == null) {
            throw new BusinessException(400, "操作人ID不能为空");
        }
        if (operationType == null || operationType.isBlank()) {
            throw new BusinessException(400, "操作类型不能为空");
        }

        AuditLog log = AuditLog.builder()
                .operatorId(operatorId)
                .operationType(operationType)
                .operationTime(LocalDateTime.now())
                .operationDetail(detail)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByOperationTimeDesc();
    }

    @Override
    public List<AuditLog> getAuditLogsByType(String operationType) {
        if (operationType == null || operationType.isBlank()) {
            throw new BusinessException(400, "操作类型不能为空");
        }
        return auditLogRepository.findByOperationTypeOrderByOperationTimeDesc(operationType);
    }
}
