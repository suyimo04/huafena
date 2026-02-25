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
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Feature: pollen-group-management, Property 5: 问卷模板版本单调递增
 * **Validates: Requirements 3.6**
 *
 * For any 问卷模板，每次保存操作应创建一个新版本，版本号严格递增，
 * 且所有历史版本保持可访问。版本总数应等于保存操作的次数。
 */
class QuestionnaireVersionProperties {

    /**
     * For any sequence of N save operations (1 create + N-1 updates),
     * version numbers are strictly 1, 2, ..., N and total version count equals N.
     */
    @Property(tries = 100)
    void versionNumbersAreStrictlyMonotonicallyIncreasing(
            @ForAll @IntRange(min = 1, max = 20) int updateCount) {

        // Track all saved versions in an in-memory list
        List<QuestionnaireVersion> savedVersions = new ArrayList<>();
        AtomicLong versionIdSeq = new AtomicLong(1);

        QuestionnaireTemplateRepository templateRepository = mock(QuestionnaireTemplateRepository.class);
        QuestionnaireVersionRepository versionRepository = mock(QuestionnaireVersionRepository.class);

        // Template mock: save returns template with id=1
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("测试模板").description("描述").createdBy(10L).build();
        when(templateRepository.save(any(QuestionnaireTemplate.class))).thenReturn(template);
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        // Version mock: save captures into savedVersions list
        when(versionRepository.save(any(QuestionnaireVersion.class))).thenAnswer(inv -> {
            QuestionnaireVersion v = inv.getArgument(0);
            v.setId(versionIdSeq.getAndIncrement());
            savedVersions.add(v);
            return v;
        });

        // findTopByTemplateIdOrderByVersionNumberDesc returns the latest saved version
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(eq(1L))).thenAnswer(inv -> {
            if (savedVersions.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(savedVersions.get(savedVersions.size() - 1));
        });

        QuestionnaireTemplateServiceImpl service =
                new QuestionnaireTemplateServiceImpl(templateRepository, versionRepository);

        // Step 1: create (produces version 1)
        CreateTemplateRequest createReq = CreateTemplateRequest.builder()
                .title("测试模板").description("描述")
                .schemaDefinition("{\"fields\":[]}")
                .build();
        service.create(createReq, 10L);

        // Step 2: N-1 updates (each produces next version)
        for (int i = 1; i < updateCount; i++) {
            UpdateTemplateRequest updateReq = UpdateTemplateRequest.builder()
                    .schemaDefinition("{\"fields\":[],\"v\":" + (i + 1) + "}")
                    .build();
            service.update(1L, updateReq);
        }

        // Assert: total version count equals number of save operations
        int totalSaveOps = updateCount; // 1 create + (updateCount - 1) updates = updateCount
        assertThat(savedVersions).hasSize(totalSaveOps);

        // Assert: version numbers are strictly 1, 2, ..., N
        for (int i = 0; i < savedVersions.size(); i++) {
            assertThat(savedVersions.get(i).getVersionNumber()).isEqualTo(i + 1);
        }

        // Assert: strictly monotonically increasing (each version > previous)
        for (int i = 1; i < savedVersions.size(); i++) {
            assertThat(savedVersions.get(i).getVersionNumber())
                    .isGreaterThan(savedVersions.get(i - 1).getVersionNumber());
        }

        // Assert: all versions are accessible (each has a unique id)
        List<Long> versionIds = savedVersions.stream()
                .map(QuestionnaireVersion::getId)
                .toList();
        assertThat(versionIds).doesNotHaveDuplicates();
    }
}
