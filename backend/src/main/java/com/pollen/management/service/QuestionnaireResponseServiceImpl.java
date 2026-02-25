package com.pollen.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.entity.QuestionnaireField;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.repository.QuestionnaireFieldRepository;
import com.pollen.management.repository.QuestionnaireResponseRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionnaireResponseServiceImpl implements QuestionnaireResponseService {

    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionnaireVersionRepository versionRepository;
    private final QuestionnaireFieldRepository fieldRepository;
    private final ConditionalLogicEvaluator conditionalLogicEvaluator;
    private final FieldValidationService fieldValidationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuestionnaireResponse submit(Long versionId, Long userId, Map<String, Object> answers) {
        // 1. Verify version exists
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(404, "问卷版本不存在"));

        // 2. Load fields for this version
        List<QuestionnaireField> fields = fieldRepository.findByVersionIdOrderBySortOrder(versionId);

        // 3. Evaluate conditional logic to determine visible fields, then validate
        List<String> allErrors = new ArrayList<>();
        for (QuestionnaireField field : fields) {
            boolean visible = conditionalLogicEvaluator.isFieldVisible(
                    field.getConditionalLogic(), answers);

            if (visible) {
                Object value = answers.get(field.getFieldKey());
                List<String> fieldErrors = fieldValidationService.validate(
                        field.getFieldKey(),
                        value,
                        Boolean.TRUE.equals(field.getRequired()),
                        field.getValidationRules());
                allErrors.addAll(fieldErrors);
            }
        }

        if (!allErrors.isEmpty()) {
            throw new BusinessException(400, String.join("; ", allErrors));
        }

        // 4. Serialize answers to JSON and store
        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "回答数据序列化失败");
        }

        QuestionnaireResponse response = QuestionnaireResponse.builder()
                .versionId(versionId)
                .userId(userId)
                .answers(answersJson)
                .build();

        return responseRepository.save(response);
    }

    @Override
    public QuestionnaireResponse getByApplicationId(Long applicationId) {
        return responseRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(404, "未找到该申请关联的问卷回答"));
    }
}
