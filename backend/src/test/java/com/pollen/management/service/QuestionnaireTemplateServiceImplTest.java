package com.pollen.management.service;

import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.UpdateTemplateRequest;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.repository.QuestionnaireTemplateRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTemplateServiceImplTest {

    @Mock
    private QuestionnaireTemplateRepository templateRepository;

    @Mock
    private QuestionnaireVersionRepository versionRepository;

    @InjectMocks
    private QuestionnaireTemplateServiceImpl service;

    private static final String SCHEMA = "{\"fields\":[{\"key\":\"name\",\"type\":\"TEXT\",\"label\":\"姓名\"}]}";

    @Test
    void create_shouldSaveTemplateAndFirstVersion() {
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .title("招募问卷")
                .description("花粉小组招募")
                .schemaDefinition(SCHEMA)
                .build();

        QuestionnaireTemplate savedTemplate = QuestionnaireTemplate.builder()
                .id(1L)
                .title("招募问卷")
                .description("花粉小组招募")
                .createdBy(10L)
                .build();

        when(templateRepository.save(any(QuestionnaireTemplate.class))).thenReturn(savedTemplate);

        QuestionnaireTemplate result = service.create(request, 10L);

        assertEquals(1L, result.getId());
        assertEquals("招募问卷", result.getTitle());

        ArgumentCaptor<QuestionnaireVersion> versionCaptor = ArgumentCaptor.forClass(QuestionnaireVersion.class);
        verify(versionRepository).save(versionCaptor.capture());
        QuestionnaireVersion savedVersion = versionCaptor.getValue();
        assertEquals(1L, savedVersion.getTemplateId());
        assertEquals(1, savedVersion.getVersionNumber());
        assertEquals(SCHEMA, savedVersion.getSchemaDefinition());
        assertEquals(VersionStatus.DRAFT, savedVersion.getStatus());
    }

    @Test
    void update_shouldCreateNewVersionWithIncrementedNumber() {
        QuestionnaireTemplate existing = QuestionnaireTemplate.builder()
                .id(1L).title("旧标题").description("旧描述").createdBy(10L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionnaireVersion latestVersion = QuestionnaireVersion.builder()
                .id(5L).templateId(1L).versionNumber(3).build();
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(1L))
                .thenReturn(Optional.of(latestVersion));

        String newSchema = "{\"fields\":[{\"key\":\"age\",\"type\":\"NUMBER\"}]}";
        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .title("新标题")
                .description("新描述")
                .schemaDefinition(newSchema)
                .build();

        QuestionnaireTemplate result = service.update(1L, request);

        assertEquals("新标题", result.getTitle());
        assertEquals("新描述", result.getDescription());

        ArgumentCaptor<QuestionnaireVersion> captor = ArgumentCaptor.forClass(QuestionnaireVersion.class);
        verify(versionRepository).save(captor.capture());
        QuestionnaireVersion newVersion = captor.getValue();
        assertEquals(4, newVersion.getVersionNumber());
        assertEquals(newSchema, newVersion.getSchemaDefinition());
        assertEquals(VersionStatus.DRAFT, newVersion.getStatus());
    }

    @Test
    void update_shouldThrowWhenTemplateNotFound() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .schemaDefinition(SCHEMA).build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(99L, request));
        assertEquals(404, ex.getCode());
    }

    @Test
    void update_shouldOnlyUpdateNonNullFields() {
        QuestionnaireTemplate existing = QuestionnaireTemplate.builder()
                .id(1L).title("原标题").description("原描述").createdBy(10L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(1L))
                .thenReturn(Optional.empty());

        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .schemaDefinition(SCHEMA).build();

        QuestionnaireTemplate result = service.update(1L, request);

        assertEquals("原标题", result.getTitle());
        assertEquals("原描述", result.getDescription());
    }

    @Test
    void update_shouldStartAtVersion1WhenNoVersionsExist() {
        QuestionnaireTemplate existing = QuestionnaireTemplate.builder()
                .id(1L).title("标题").createdBy(10L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(1L))
                .thenReturn(Optional.empty());

        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .schemaDefinition(SCHEMA).build();

        service.update(1L, request);

        ArgumentCaptor<QuestionnaireVersion> captor = ArgumentCaptor.forClass(QuestionnaireVersion.class);
        verify(versionRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getVersionNumber());
    }

    @Test
    void getById_shouldReturnTemplate() {
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("测试").build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        QuestionnaireTemplate result = service.getById(1L);
        assertEquals("测试", result.getTitle());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getById(99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void listAll_shouldReturnAllTemplates() {
        List<QuestionnaireTemplate> templates = List.of(
                QuestionnaireTemplate.builder().id(1L).title("A").build(),
                QuestionnaireTemplate.builder().id(2L).title("B").build()
        );
        when(templateRepository.findAll()).thenReturn(templates);

        List<QuestionnaireTemplate> result = service.listAll();
        assertEquals(2, result.size());
    }

    @Test
    void listAll_shouldReturnEmptyListWhenNoTemplates() {
        when(templateRepository.findAll()).thenReturn(Collections.emptyList());

        List<QuestionnaireTemplate> result = service.listAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void delete_shouldRemoveTemplateAndAllVersions() {
        when(templateRepository.existsById(1L)).thenReturn(true);
        List<QuestionnaireVersion> versions = List.of(
                QuestionnaireVersion.builder().id(1L).templateId(1L).versionNumber(1).build(),
                QuestionnaireVersion.builder().id(2L).templateId(1L).versionNumber(2).build()
        );
        when(versionRepository.findByTemplateIdOrderByVersionNumberDesc(1L)).thenReturn(versions);

        service.delete(1L);

        verify(versionRepository).deleteAll(versions);
        verify(templateRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowWhenTemplateNotFound() {
        when(templateRepository.existsById(99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.delete(99L));
        assertEquals(404, ex.getCode());
    }

    // --- publish tests ---

    @Test
    void publish_shouldSetStatusPublishedAndUpdateActiveVersionId() {
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("问卷").createdBy(10L).build();
        QuestionnaireVersion version = QuestionnaireVersion.builder()
                .id(5L).templateId(1L).versionNumber(2).status(VersionStatus.DRAFT).build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(versionRepository.findById(5L)).thenReturn(Optional.of(version));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionnaireVersion result = service.publish(1L, 5L);

        assertEquals(VersionStatus.PUBLISHED, result.getStatus());
        assertEquals(5L, template.getActiveVersionId());
        verify(versionRepository).save(version);
        verify(templateRepository).save(template);
    }

    @Test
    void publish_shouldThrowWhenTemplateNotFound() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.publish(99L, 1L));
        assertEquals(404, ex.getCode());
        assertEquals("问卷模板不存在", ex.getMessage());
    }

    @Test
    void publish_shouldThrowWhenVersionNotFound() {
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("问卷").createdBy(10L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(versionRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.publish(1L, 99L));
        assertEquals(404, ex.getCode());
        assertEquals("问卷版本不存在", ex.getMessage());
    }

    @Test
    void publish_shouldThrowWhenVersionDoesNotBelongToTemplate() {
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("问卷").createdBy(10L).build();
        QuestionnaireVersion version = QuestionnaireVersion.builder()
                .id(5L).templateId(2L).versionNumber(1).status(VersionStatus.DRAFT).build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(versionRepository.findById(5L)).thenReturn(Optional.of(version));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.publish(1L, 5L));
        assertEquals(400, ex.getCode());
        assertEquals("该版本不属于指定的问卷模板", ex.getMessage());
    }

    // --- getVersionHistory tests ---

    @Test
    void getVersionHistory_shouldReturnVersionsOrderedByVersionNumberDesc() {
        when(templateRepository.existsById(1L)).thenReturn(true);
        List<QuestionnaireVersion> versions = List.of(
                QuestionnaireVersion.builder().id(3L).templateId(1L).versionNumber(3).build(),
                QuestionnaireVersion.builder().id(2L).templateId(1L).versionNumber(2).build(),
                QuestionnaireVersion.builder().id(1L).templateId(1L).versionNumber(1).build()
        );
        when(versionRepository.findByTemplateIdOrderByVersionNumberDesc(1L)).thenReturn(versions);

        List<QuestionnaireVersion> result = service.getVersionHistory(1L);

        assertEquals(3, result.size());
        assertEquals(3, result.get(0).getVersionNumber());
        assertEquals(2, result.get(1).getVersionNumber());
        assertEquals(1, result.get(2).getVersionNumber());
    }

    @Test
    void getVersionHistory_shouldReturnEmptyListWhenNoVersions() {
        when(templateRepository.existsById(1L)).thenReturn(true);
        when(versionRepository.findByTemplateIdOrderByVersionNumberDesc(1L))
                .thenReturn(Collections.emptyList());

        List<QuestionnaireVersion> result = service.getVersionHistory(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void getVersionHistory_shouldThrowWhenTemplateNotFound() {
        when(templateRepository.existsById(99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getVersionHistory(99L));
        assertEquals(404, ex.getCode());
        assertEquals("问卷模板不存在", ex.getMessage());
    }

    // --- getVersion tests ---

    @Test
    void getVersion_shouldReturnVersionById() {
        QuestionnaireVersion version = QuestionnaireVersion.builder()
                .id(5L).templateId(1L).versionNumber(2)
                .schemaDefinition(SCHEMA).status(VersionStatus.DRAFT).build();
        when(versionRepository.findById(5L)).thenReturn(Optional.of(version));

        QuestionnaireVersion result = service.getVersion(5L);

        assertEquals(5L, result.getId());
        assertEquals(2, result.getVersionNumber());
        assertEquals(SCHEMA, result.getSchemaDefinition());
    }

    @Test
    void getVersion_shouldThrowWhenVersionNotFound() {
        when(versionRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getVersion(99L));
        assertEquals(404, ex.getCode());
        assertEquals("问卷版本不存在", ex.getMessage());
    }
}
