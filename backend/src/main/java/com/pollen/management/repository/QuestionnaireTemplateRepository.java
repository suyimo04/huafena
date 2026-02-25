package com.pollen.management.repository;

import com.pollen.management.entity.QuestionnaireTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionnaireTemplateRepository extends JpaRepository<QuestionnaireTemplate, Long> {
}
