package com.pollen.management.repository;

import com.pollen.management.entity.ActivityMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityMaterialRepository extends JpaRepository<ActivityMaterial, Long> {
    List<ActivityMaterial> findByActivityId(Long activityId);
}
