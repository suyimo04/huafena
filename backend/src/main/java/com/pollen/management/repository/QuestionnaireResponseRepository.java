package com.pollen.management.repository;

import com.pollen.management.entity.QuestionnaireResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponse, Long> {
    Optional<QuestionnaireResponse> findByApplicationId(Long applicationId);
}
