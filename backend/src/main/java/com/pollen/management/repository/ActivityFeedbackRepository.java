package com.pollen.management.repository;

import com.pollen.management.entity.ActivityFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityFeedbackRepository extends JpaRepository<ActivityFeedback, Long> {
    List<ActivityFeedback> findByActivityId(Long activityId);
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
}
