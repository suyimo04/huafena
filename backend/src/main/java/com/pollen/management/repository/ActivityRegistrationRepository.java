package com.pollen.management.repository;

import com.pollen.management.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
    Optional<ActivityRegistration> findByActivityIdAndUserId(Long activityId, Long userId);
}
