package com.pollen.management.repository;

import com.pollen.management.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    List<UserActivityLog> findByUserId(Long userId);

    List<UserActivityLog> findByUserIdAndActionTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<UserActivityLog> findByUserIdOrderByActionTimeDesc(Long userId);
}
