package com.pollen.management.repository;

import com.pollen.management.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    long countByActivityTimeBetween(LocalDateTime start, LocalDateTime end);
}
