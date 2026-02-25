package com.pollen.management.repository;

import com.pollen.management.entity.Application;
import com.pollen.management.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByOrderByCreatedAtDesc();
    List<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
    List<Application> findByUserId(Long userId);
    boolean existsByUserIdAndStatusIn(Long userId, List<ApplicationStatus> statuses);
    long countByStatus(ApplicationStatus status);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
