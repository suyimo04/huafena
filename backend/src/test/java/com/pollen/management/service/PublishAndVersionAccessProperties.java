package com.pollen.management.service;

import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.UpdateTemplateRequest;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.repository.QuestionnaireTemplateRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 6: 发布后活跃版本正确性
 * Feature: pollen-group-management, Property 7: 历史版本完整可访问
 * **Validates: Requirements 3.7, 3.8**
 */
class PublishAndVersionAccessProperties {

    /**
     * Property 6: 发布后活跃版本正确性
     *
     * For any 问卷模板，发布某个版本后，该模板的 active_version_id 应指向被发布的版本。
     *
     * Strategy: Create a template with N versions (1 create + N-1 updates), then publish
     * a randomly chosen version index. After publish, verify template.activeVersionId == published version id.
     */
    @Property(tries = 100)
    void afterPublishActiveVersionIdPointsToPublishedVersion(
            @ForAll @IntRange(min = 1, max = 10) int totalVersions,
            @ForAll @IntRange(min = 0, max = 9) int publishIndexRaw) {

        // Clamp publishIndex to valid range
        int publishIndex = publishIndexRaw % totalVersions;

        List<QuestionnaireVersion> savedVersions = new ArrayList<>();
        AtomicLong versionIdSeq = new AtomicLong(1);

        QuestionnaireTemplateRepository templateRepository = mock(QuestionnaireTemplateRepository.class);
        QuestionnaireVersionRepository versionRepository = mock(QuestionnaireVersionRepository.class);

        // Template mock
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("测试模板").description("描述").createdBy(10L).build();
        when(templateRepository.save(any(QuestionnaireTemplate.class))).thenAnswer(inv -> {
            QuestionnaireTemplate t = inv.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.existsById(1L)).thenReturn(true);

        // Version mock: save captures into savedVersions
        when(versionRepository.save(any(QuestionnaireVersion.class))).thenAnswer(inv -> {
            QuestionnaireVersion v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId(versionIdSeq.getAndIncrement());
                savedVersions.add(v);
            }
            return v;
        });

        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(eq(1L))).thenAnswer(inv -> {
            if (savedVersions.isEmpty()) return Optional.empty();
            return Optional.of(savedVersions.get(savedVersions.size() - 1));
        });

        // findById for versions delegates to savedVersions
        when(versionRepository.findById(any(Long.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return savedVersions.stream().filter(v -> v.getId().equals(id)).findFirst();
        });

        QuestionnaireTemplateServiceImpl service =
                new QuestionnaireTemplateServiceImpl(templateRepository, versionRepository);

        // Create template (version 1)
        service.create(CreateTemplateRequest.builder()
                .title("测试模板").description("描述")
                .schemaDefinition("{\"fields\":[]}")
                .build(), 10L);

        // Create additional versions via update
        for (int i = 1; i < totalVersions; i++) {
            service.update(1L, UpdateTemplateRequest.builder()
                    .schemaDefinition("{\"fields\":[],\"v\":" + (i + 1) + "}")
                    .build());
        }

        // Pick the version to publish
        QuestionnaireVersion versionToPublish = savedVersions.get(publishIndex);

        // Publish
        service.publish(1L, versionToPublish.getId());

        // Assert: template.activeVersionId == published version's id
        assertThat(template.getActiveVersionId()).isEqualTo(versionToPublish.getId());
    }

    /**
     * Property 7: 历史版本完整可访问
     *
     * For any 拥有 N 个版本的问卷模板，查询任意版本 1 到 N 应返回该版本的完整 schema_definition 配置。
     *
     * Strategy: Create a template with N versions, then call getVersionHistory and getVersion
     * for each. Verify all N versions are returned and each has a non-null schemaDefinition.
     */
    @Property(tries = 100)
    void allHistoricalVersionsAreAccessibleWithCompleteSchema(
            @ForAll @IntRange(min = 1, max = 15) int totalVersions) {

        List<QuestionnaireVersion> savedVersions = new ArrayList<>();
        AtomicLong versionIdSeq = new AtomicLong(1);

        QuestionnaireTemplateRepository templateRepository = mock(QuestionnaireTemplateRepository.class);
        QuestionnaireVersionRepository versionRepository = mock(QuestionnaireVersionRepository.class);

        // Template mock
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("测试模板").description("描述").createdBy(10L).build();
        when(templateRepository.save(any(QuestionnaireTemplate.class))).thenAnswer(inv -> {
            QuestionnaireTemplate t = inv.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.existsById(1L)).thenReturn(true);

        // Version mock
        when(versionRepository.save(any(QuestionnaireVersion.class))).thenAnswer(inv -> {
            QuestionnaireVersion v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId(versionIdSeq.getAndIncrement());
                savedVersions.add(v);
            }
            return v;
        });

        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(eq(1L))).thenAnswer(inv -> {
            if (savedVersions.isEmpty()) return Optional.empty();
            return Optional.of(savedVersions.get(savedVersions.size() - 1));
        });

        // getVersionHistory returns all saved versions in descending order
        when(versionRepository.findByTemplateIdOrderByVersionNumberDesc(eq(1L))).thenAnswer(inv -> {
            List<QuestionnaireVersion> reversed = new ArrayList<>(savedVersions);
            reversed.sort((a, b) -> Integer.compare(b.getVersionNumber(), a.getVersionNumber()));
            return reversed;
        });

        // findById for individual version access
        when(versionRepository.findById(any(Long.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return savedVersions.stream().filter(v -> v.getId().equals(id)).findFirst();
        });

        QuestionnaireTemplateServiceImpl service =
                new QuestionnaireTemplateServiceImpl(templateRepository, versionRepository);

        // Create template (version 1)
        service.create(CreateTemplateRequest.builder()
                .title("测试模板").description("描述")
                .schemaDefinition("{\"fields\":[],\"v\":1}")
                .build(), 10L);

        // Create additional versions
        for (int i = 1; i < totalVersions; i++) {
            service.update(1L, UpdateTemplateRequest.builder()
                    .schemaDefinition("{\"fields\":[],\"v\":" + (i + 1) + "}")
                    .build());
        }

        // Verify via getVersionHistory: returns exactly N versions
        List<QuestionnaireVersion> history = service.getVersionHistory(1L);
        assertThat(history).hasSize(totalVersions);

        // Verify each version is individually accessible with non-null schemaDefinition
        for (QuestionnaireVersion v : savedVersions) {
            QuestionnaireVersion retrieved = service.getVersion(v.getId());
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getSchemaDefinition()).isNotNull();
            assertThat(retrieved.getSchemaDefinition()).isNotEmpty();
        }

        // Verify all version numbers from 1 to N are present
        List<Integer> versionNumbers = history.stream()
                .map(QuestionnaireVersion::getVersionNumber)
                .sorted()
                .toList();
        for (int i = 0; i < totalVersions; i++) {
            assertThat(versionNumbers.get(i)).isEqualTo(i + 1);
        }
    }
}
