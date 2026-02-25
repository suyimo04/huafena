package com.pollen.management.repository;

import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoleChangeHistoryRepository extends JpaRepository<RoleChangeHistory, Long> {

    List<RoleChangeHistory> findByUserIdOrderByChangedAtDesc(Long userId);

    List<RoleChangeHistory> findByChangedByOrderByChangedAtDesc(String changedBy);

    long countByNewRoleAndChangedAtBetween(Role newRole, LocalDateTime start, LocalDateTime end);
}
