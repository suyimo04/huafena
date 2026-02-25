package com.pollen.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.entity.QuestionnaireField;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.FieldType;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.repository.QuestionnaireFieldRepository;
import com.pollen.management.repository.QuestionnaireResponseRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireResponseServiceImplTest {

    @Mock
    private QuestionnaireResponseRepository responseRepository;
    @Mock
    private QuestionnaireVersionRepository versionRepository;
    @Mock
    private QuestionnaireFieldRepository fieldRepository;

    private ConditionalLogicEvaluator conditionalLogicEvaluator;
    private FieldValidationService fieldValidationService;
    private ObjectMapper objectMapper;

    private QuestionnaireResponseServiceImpl service;

    @BeforeEach
    void setUp() {
        conditionalLogicEvaluator = new ConditionalLogicEvaluator();
        fieldValidationService = new FieldValidationService();
        objectMapper = new ObjectMapper();
        service = new QuestionnaireResponseServiceImpl(
                responseRepository, versionRepository, fieldRepository,
                conditionalLogicEvaluator, fieldValidationService, objectMapper);
    }

    // --- Helper methods ---

    private QuestionnaireVersion buildVersion(Long id) {
        return QuestionnaireVersion.builder()
                .id(id)
                .templateId(1L)
                .versionNumber(1)
                .status(VersionStatus.PUBLISHED)
                .build();
    }

    private QuestionnaireField buildField(String key, FieldType type, boolean required,
                                          String validationRules, String conditionalLogic) {
        return QuestionnaireField.builder()
                .id(1L)
                .versionId(10L)
                .fieldKey(key)
                .fieldType(type)
                .label(key)
                .required(required)
                .validationRules(validationRules)
                .conditionalLogic(conditionalLogic)
                .sortOrder(0)
                .build();
    }

    // --- submit tests ---

    @Test
    void submit_shouldThrowWhenVersionNotFound() {
        when(versionRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(99L, 1L, Map.of("name", "test")));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("问卷版本不存在"));
    }

    @Test
    void submit_shouldSaveResponseWithValidAnswers() {
        Long versionId = 10L;
        Long userId = 1L;
        Map<String, Object> answers = new LinkedHashMap<>();
        answers.put("name", "张三");

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("name", FieldType.TEXT, true,
                        "{\"minLength\":1,\"maxLength\":20}", null)
        ));
        when(responseRepository.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        QuestionnaireResponse result = service.submit(versionId, userId, answers);

        assertNotNull(result);
        assertEquals(versionId, result.getVersionId());
        assertEquals(userId, result.getUserId());
        assertTrue(result.getAnswers().contains("张三"));

        ArgumentCaptor<QuestionnaireResponse> captor = ArgumentCaptor.forClass(QuestionnaireResponse.class);
        verify(responseRepository).save(captor.capture());
        assertEquals(versionId, captor.getValue().getVersionId());
    }

    @Test
    void submit_shouldFailValidationForRequiredFieldMissing() {
        Long versionId = 10L;
        Map<String, Object> answers = new HashMap<>(); // no "name" key

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("name", FieldType.TEXT, true, null, null)
        ));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(versionId, 1L, answers));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void submit_shouldSkipValidationForHiddenFields() {
        Long versionId = 10L;
        // "school" is required but hidden because is_student != "yes"
        String conditionalLogic = "{\"action\":\"SHOW\",\"conditions\":[{\"fieldKey\":\"is_student\",\"operator\":\"EQUALS\",\"value\":\"yes\"}]}";

        Map<String, Object> answers = new LinkedHashMap<>();
        answers.put("is_student", "no");
        // "school" is not provided, but it's hidden so should pass

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("is_student", FieldType.SINGLE_CHOICE, true, null, null),
                buildField("school", FieldType.TEXT, true,
                        "{\"minLength\":2,\"maxLength\":50}", conditionalLogic)
        ));
        when(responseRepository.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(101L);
                    return r;
                });

        QuestionnaireResponse result = service.submit(versionId, 1L, answers);
        assertNotNull(result);
        verify(responseRepository).save(any());
    }

    @Test
    void submit_shouldValidateVisibleConditionalFields() {
        Long versionId = 10L;
        String conditionalLogic = "{\"action\":\"SHOW\",\"conditions\":[{\"fieldKey\":\"is_student\",\"operator\":\"EQUALS\",\"value\":\"yes\"}]}";

        Map<String, Object> answers = new LinkedHashMap<>();
        answers.put("is_student", "yes");
        // "school" is visible and required but not provided

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("is_student", FieldType.SINGLE_CHOICE, true, null, null),
                buildField("school", FieldType.TEXT, true,
                        "{\"minLength\":2,\"maxLength\":50}", conditionalLogic)
        ));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(versionId, 1L, answers));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("school"));
    }

    @Test
    void submit_shouldFailValidationRules() {
        Long versionId = 10L;
        Map<String, Object> answers = Map.of("name", "A"); // too short, minLength=2

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("name", FieldType.TEXT, true,
                        "{\"minLength\":2,\"maxLength\":20}", null)
        ));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(versionId, 1L, answers));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void submit_shouldStoreAnswersAsJson() throws Exception {
        Long versionId = 10L;
        Map<String, Object> answers = new LinkedHashMap<>();
        answers.put("name", "李四");
        answers.put("age", 25);

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("name", FieldType.TEXT, true, null, null),
                buildField("age", FieldType.NUMBER, false, null, null)
        ));
        when(responseRepository.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(102L);
                    return r;
                });

        QuestionnaireResponse result = service.submit(versionId, 1L, answers);

        // Verify stored JSON can be deserialized back
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> parsed = mapper.readValue(result.getAnswers(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        assertEquals("李四", parsed.get("name"));
        assertEquals(25, parsed.get("age"));
    }

    @Test
    void submit_shouldAssociateVersionId() {
        Long versionId = 10L;
        Map<String, Object> answers = Map.of("name", "王五");

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("name", FieldType.TEXT, true, null, null)
        ));
        when(responseRepository.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(103L);
                    return r;
                });

        QuestionnaireResponse result = service.submit(versionId, 1L, answers);
        assertEquals(versionId, result.getVersionId());
    }

    @Test
    void submit_shouldPassWithNoFieldsDefined() {
        Long versionId = 10L;
        Map<String, Object> answers = Map.of("extra", "data");

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of());
        when(responseRepository.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(104L);
                    return r;
                });

        QuestionnaireResponse result = service.submit(versionId, 1L, answers);
        assertNotNull(result);
    }

    @Test
    void submit_shouldCollectMultipleValidationErrors() {
        Long versionId = 10L;
        Map<String, Object> answers = new HashMap<>(); // both required fields missing

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepository.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of(
                buildField("name", FieldType.TEXT, true, null, null),
                buildField("email", FieldType.TEXT, true, null, null)
        ));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submit(versionId, 1L, answers));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("name"));
        assertTrue(ex.getMessage().contains("email"));
    }

    // --- getByApplicationId tests ---

    @Test
    void getByApplicationId_shouldReturnResponse() {
        Long applicationId = 5L;
        QuestionnaireResponse expected = QuestionnaireResponse.builder()
                .id(1L)
                .versionId(10L)
                .userId(1L)
                .applicationId(applicationId)
                .answers("{\"name\":\"test\"}")
                .build();

        when(responseRepository.findByApplicationId(applicationId))
                .thenReturn(Optional.of(expected));

        QuestionnaireResponse result = service.getByApplicationId(applicationId);
        assertEquals(expected.getId(), result.getId());
        assertEquals(applicationId, result.getApplicationId());
    }

    @Test
    void getByApplicationId_shouldThrowWhenNotFound() {
        when(responseRepository.findByApplicationId(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getByApplicationId(99L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("未找到"));
    }
}
