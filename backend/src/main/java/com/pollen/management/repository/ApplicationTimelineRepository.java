package com.pollen.management.repository;

import com.pollen.management.entity.ApplicationTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationTimelineRepository extends JpaRepository<ApplicationTimeline, Long> {
    List<ApplicationTimeline> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
