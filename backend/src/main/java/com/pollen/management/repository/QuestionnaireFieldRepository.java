package com.pollen.management.repository;

import com.pollen.management.entity.QuestionnaireField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionnaireFieldRepository extends JpaRepository<QuestionnaireField, Long> {
    List<QuestionnaireField> findByVersionIdOrderBySortOrder(Long versionId);
}
