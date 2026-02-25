package com.pollen.management.repository;

import com.pollen.management.entity.ActivityStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityStatisticsRepository extends JpaRepository<ActivityStatistics, Long> {
    Optional<ActivityStatistics> findByActivityId(Long activityId);
}
