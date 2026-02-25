package com.pollen.management.repository;

import com.pollen.management.entity.ActivityGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityGroupRepository extends JpaRepository<ActivityGroup, Long> {
    List<ActivityGroup> findByActivityId(Long activityId);
}
