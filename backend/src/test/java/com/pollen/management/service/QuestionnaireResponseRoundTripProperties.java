package com.pollen.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.entity.QuestionnaireField;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.FieldType;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.repository.QuestionnaireFieldRepository;
import com.pollen.management.repository.QuestionnaireResponseRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 10: 问卷回答数据往返一致性
 * **Validates: Requirements 3.11**
 *
 * For any 有效的问卷回答数据，提交后再查询应返回与原始提交相同的回答内容，
 * 且关联的问卷版本 ID 正确。
 *
 * The QuestionnaireResponseServiceImpl serializes answers as JSON via ObjectMapper
 * and stores them in the entity. This property verifies that for any valid answers map,
 * the stored JSON can be deserialized back to the original data (round-trip integrity).
 */
class QuestionnaireResponseRoundTripProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---- Helpers ----

    private QuestionnaireResponseServiceImpl createService(
            QuestionnaireResponseRepository responseRepo,
            QuestionnaireVersionRepository versionRepo,
            QuestionnaireFieldRepository fieldRepo) {
        return new QuestionnaireResponseServiceImpl(
                responseRepo, versionRepo, fieldRepo,
                new ConditionalLogicEvaluator(),
                new FieldValidationService(),
                objectMapper);
    }

    private QuestionnaireVersion buildVersion(Long id) {
        return QuestionnaireVersion.builder()
                .id(id)
                .templateId(1L)
                .versionNumber(1)
                .status(VersionStatus.PUBLISHED)
                .build();
    }

    // ---- Generators ----

    @Provide
    Arbitrary<String> fieldKeys() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(15);
    }

    @Provide
    Arbitrary<String> textValues() {
        // Generate non-blank text values suitable for TEXT fields
        return Arbitraries.strings()
                .ofMinLength(2).ofMaxLength(50)
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<Map<String, Object>> answersMap() {
        // Generate maps with 1-5 entries, each with a unique field key and a simple value
        Arbitrary<String> keys = Arbitraries.of(
                "field_a", "field_b", "field_c", "field_d", "field_e",
                "field_f", "field_g", "field_h");

        Arbitrary<Object> values = Arbitraries.oneOf(
                Arbitraries.strings().ofMinLength(2).ofMaxLength(30)
                        .filter(s -> !s.isBlank())
                        .map(s -> (Object) s),
                Arbitraries.integers().between(0, 10000).map(i -> (Object) i),
                Arbitraries.doubles().between(0.0, 1000.0)
                        .map(d -> (Object) (Math.round(d * 100.0) / 100.0)),
                Arbitraries.of(true, false).map(b -> (Object) b)
        );

        return Arbitraries.maps(keys, values)
                .ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<Long> versionIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    // ---- Property: Round-trip JSON serialization consistency ----

    /**
     * For any valid answers map, submitting via the service serializes answers to JSON.
     * Deserializing that JSON back should produce data equivalent to the original answers.
     * The version ID on the stored response must match the submitted version ID.
     */
    @Property(tries = 100)
    void answersRoundTripThroughSubmitAndDeserialize(
            @ForAll("answersMap") Map<String, Object> answers,
            @ForAll("versionIds") Long versionId,
            @ForAll("userIds") Long userId) throws Exception {

        // Mock repositories
        QuestionnaireResponseRepository responseRepo = mock(QuestionnaireResponseRepository.class);
        QuestionnaireVersionRepository versionRepo = mock(QuestionnaireVersionRepository.class);
        QuestionnaireFieldRepository fieldRepo = mock(QuestionnaireFieldRepository.class);

        // Version exists
        when(versionRepo.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));

        // No fields defined — so no validation is needed, any answers map is valid
        when(fieldRepo.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of());

        // Capture the saved response
        when(responseRepo.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        QuestionnaireResponseServiceImpl service = createService(responseRepo, versionRepo, fieldRepo);

        // Submit
        QuestionnaireResponse response = service.submit(versionId, userId, answers);

        // Verify version ID association
        assertThat(response.getVersionId()).isEqualTo(versionId);
        assertThat(response.getUserId()).isEqualTo(userId);

        // Round-trip: deserialize stored JSON back to a map
        String storedJson = response.getAnswers();
        assertThat(storedJson).isNotNull().isNotBlank();

        Map<String, Object> deserialized = objectMapper.readValue(
                storedJson, new TypeReference<Map<String, Object>>() {});

        // Verify all original keys are present and values match
        assertThat(deserialized).hasSameSizeAs(answers);
        for (Map.Entry<String, Object> entry : answers.entrySet()) {
            assertThat(deserialized).containsKey(entry.getKey());
            // Compare via string representation to handle numeric type coercion
            // (e.g., Integer vs Long after JSON round-trip)
            assertThat(String.valueOf(deserialized.get(entry.getKey())))
                    .isEqualTo(String.valueOf(entry.getValue()));
        }
    }

    // ---- Property: JSON round-trip is idempotent ----

    /**
     * Serializing answers to JSON and deserializing back, then serializing again,
     * should produce the same JSON string (idempotent round-trip).
     */
    @Property(tries = 100)
    void jsonSerializationIsIdempotent(
            @ForAll("answersMap") Map<String, Object> answers) throws Exception {

        // First round-trip
        String json1 = objectMapper.writeValueAsString(answers);
        Map<String, Object> deserialized = objectMapper.readValue(
                json1, new TypeReference<Map<String, Object>>() {});

        // Second serialization from deserialized data
        String json2 = objectMapper.writeValueAsString(deserialized);

        // The two JSON strings should be identical
        assertThat(json2).isEqualTo(json1);
    }

    // ---- Property: Stored response preserves all answer keys ----

    /**
     * For any answers map submitted through the service, every key from the original
     * map must be present in the stored JSON, ensuring no data loss.
     */
    @Property(tries = 100)
    void allAnswerKeysPreservedAfterSubmit(
            @ForAll("answersMap") Map<String, Object> answers,
            @ForAll("versionIds") Long versionId) throws Exception {

        QuestionnaireResponseRepository responseRepo = mock(QuestionnaireResponseRepository.class);
        QuestionnaireVersionRepository versionRepo = mock(QuestionnaireVersionRepository.class);
        QuestionnaireFieldRepository fieldRepo = mock(QuestionnaireFieldRepository.class);

        when(versionRepo.findById(versionId)).thenReturn(Optional.of(buildVersion(versionId)));
        when(fieldRepo.findByVersionIdOrderBySortOrder(versionId)).thenReturn(List.of());
        when(responseRepo.save(any(QuestionnaireResponse.class)))
                .thenAnswer(inv -> {
                    QuestionnaireResponse r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        QuestionnaireResponseServiceImpl service = createService(responseRepo, versionRepo, fieldRepo);
        QuestionnaireResponse response = service.submit(versionId, 1L, answers);

        Map<String, Object> deserialized = objectMapper.readValue(
                response.getAnswers(), new TypeReference<Map<String, Object>>() {});

        assertThat(deserialized.keySet()).containsExactlyInAnyOrderElementsOf(answers.keySet());
    }
}
