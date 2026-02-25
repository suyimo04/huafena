package com.pollen.management.repository;

import com.pollen.management.entity.QuestionnaireVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireVersionRepository extends JpaRepository<QuestionnaireVersion, Long> {
    List<QuestionnaireVersion> findByTemplateIdOrderByVersionNumberDesc(Long templateId);
    Optional<QuestionnaireVersion> findTopByTemplateIdOrderByVersionNumberDesc(Long templateId);
}
