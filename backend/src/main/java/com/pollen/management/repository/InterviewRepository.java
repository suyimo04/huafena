package com.pollen.management.repository;

import com.pollen.management.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Optional<Interview> findByApplicationId(Long applicationId);
    long countByCompletedAtBetween(LocalDateTime start, LocalDateTime end);
}
