package com.pollen.management.repository;

import com.pollen.management.entity.PointsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {
    List<PointsRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PointsRecord> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
