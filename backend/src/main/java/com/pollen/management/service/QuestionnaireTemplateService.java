package com.pollen.management.service;

import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.UpdateTemplateRequest;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;

import java.util.List;

public interface QuestionnaireTemplateService {

    QuestionnaireTemplate create(CreateTemplateRequest request, Long createdBy);

    QuestionnaireTemplate update(Long id, UpdateTemplateRequest request);

    QuestionnaireTemplate getById(Long id);

    List<QuestionnaireTemplate> listAll();

    void delete(Long id);

    QuestionnaireVersion publish(Long templateId, Long versionId);

    List<QuestionnaireVersion> getVersionHistory(Long templateId);

    QuestionnaireVersion getVersion(Long versionId);
}

