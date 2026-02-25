package com.pollen.management.repository;

import com.pollen.management.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByOperationTimeDesc();
    List<AuditLog> findByOperationTypeOrderByOperationTimeDesc(String operationType);
}
