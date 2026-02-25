package com.pollen.management.service;

import com.pollen.management.config.RedisConfig;
import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.UpdateTemplateRequest;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.repository.QuestionnaireTemplateRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionnaireTemplateServiceImpl implements QuestionnaireTemplateService {

    private final QuestionnaireTemplateRepository templateRepository;
    private final QuestionnaireVersionRepository versionRepository;

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_QUESTIONNAIRE, allEntries = true)
    public QuestionnaireTemplate create(CreateTemplateRequest request, Long createdBy) {
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();
        template = templateRepository.save(template);

        QuestionnaireVersion version = QuestionnaireVersion.builder()
                .templateId(template.getId())
                .versionNumber(1)
                .schemaDefinition(request.getSchemaDefinition())
                .status(VersionStatus.DRAFT)
                .build();
        versionRepository.save(version);

        return template;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_QUESTIONNAIRE, allEntries = true)
    public QuestionnaireTemplate update(Long id, UpdateTemplateRequest request) {
        QuestionnaireTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "问卷模板不存在"));

        if (request.getTitle() != null) {
            template.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        template = templateRepository.save(template);

        int nextVersionNumber = versionRepository
                .findTopByTemplateIdOrderByVersionNumberDesc(id)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        QuestionnaireVersion version = QuestionnaireVersion.builder()
                .templateId(id)
                .versionNumber(nextVersionNumber)
                .schemaDefinition(request.getSchemaDefinition())
                .status(VersionStatus.DRAFT)
                .build();
        versionRepository.save(version);

        return template;
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_QUESTIONNAIRE, key = "#id", unless = "#result == null")
    public QuestionnaireTemplate getById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "问卷模板不存在"));
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_QUESTIONNAIRE, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<QuestionnaireTemplate> listAll() {
        return templateRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_QUESTIONNAIRE, allEntries = true)
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new BusinessException(404, "问卷模板不存在");
        }
        List<QuestionnaireVersion> versions = versionRepository
                .findByTemplateIdOrderByVersionNumberDesc(id);
        versionRepository.deleteAll(versions);
        templateRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_QUESTIONNAIRE, allEntries = true)
    public QuestionnaireVersion publish(Long templateId, Long versionId) {
        QuestionnaireTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "问卷模板不存在"));

        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(404, "问卷版本不存在"));

        if (!version.getTemplateId().equals(templateId)) {
            throw new BusinessException(400, "该版本不属于指定的问卷模板");
        }

        version.setStatus(VersionStatus.PUBLISHED);
        versionRepository.save(version);

        template.setActiveVersionId(versionId);
        templateRepository.save(template);

        return version;
    }

    @Override
    public List<QuestionnaireVersion> getVersionHistory(Long templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new BusinessException(404, "问卷模板不存在");
        }
        return versionRepository.findByTemplateIdOrderByVersionNumberDesc(templateId);
    }

    @Override
    public QuestionnaireVersion getVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(404, "问卷版本不存在"));
    }
}
