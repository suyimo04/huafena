package com.pollen.management.service;

import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.repository.PublicLinkRepository;
import com.pollen.management.repository.QuestionnaireTemplateRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicLinkServiceImpl implements PublicLinkService {

    private final PublicLinkRepository publicLinkRepository;
    private final QuestionnaireVersionRepository versionRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final QuestionnaireResponseService responseService;

    @Override
    public PublicLink getActiveLink(String linkToken) {
        PublicLink link = publicLinkRepository.findByLinkToken(linkToken)
                .orElseThrow(() -> new BusinessException(404, "链接无效或已过期"));

        if (!Boolean.TRUE.equals(link.getActive())) {
            throw new BusinessException(404, "链接无效或已过期");
        }

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(404, "链接无效或已过期");
        }

        return link;
    }

    @Override
    public QuestionnaireVersion getQuestionnaireByToken(String linkToken) {
        PublicLink link = getActiveLink(linkToken);
        return versionRepository.findById(link.getVersionId())
                .orElseThrow(() -> new BusinessException(404, "问卷版本不存在"));
    }

    @Override
    public Map<String, Object> submitByToken(String linkToken, Map<String, Object> answers) {
        PublicLink link = getActiveLink(linkToken);

        // Submit the questionnaire response with a placeholder userId (0 for public submissions)
        // The actual user creation and application record creation will be handled
        // by the ApplicationService in the recruitment pipeline (Task 7)
        responseService.submit(link.getVersionId(), 0L, answers);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "问卷提交成功");
        result.put("linkToken", linkToken);
        return result;
    }

    @Override
    public PublicLink generate(Long templateId, Long createdBy) {
        QuestionnaireTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "问卷模板不存在"));

        if (template.getActiveVersionId() == null) {
            throw new BusinessException(400, "问卷模板尚未发布");
        }

        PublicLink link = PublicLink.builder()
                .linkToken(UUID.randomUUID().toString())
                .templateId(templateId)
                .versionId(template.getActiveVersionId())
                .createdBy(createdBy)
                .active(true)
                .build();

        return publicLinkRepository.save(link);
    }

    @Override
    public List<PublicLink> listAll() {
        return publicLinkRepository.findAll();
    }
}
