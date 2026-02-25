package com.pollen.management.repository;

import com.pollen.management.entity.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {

    List<BackupRecord> findAllByOrderByCreatedAtDesc();

    List<BackupRecord> findByCreatedAtBefore(LocalDateTime dateTime);
}
