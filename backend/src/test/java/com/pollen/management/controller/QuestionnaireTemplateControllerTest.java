package com.pollen.management.controller;

import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.PublishVersionRequest;
import com.pollen.management.dto.UpdateTemplateRequest;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.service.QuestionnaireTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTemplateControllerTest {

    @Mock
    private QuestionnaireTemplateService templateService;

    @InjectMocks
    private QuestionnaireTemplateController controller;

    private Authentication mockAuth() {
        var auth = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        auth.setDetails(1L);
        return auth;
    }

    @Test
    void create_shouldDelegateToServiceAndReturnSuccess() {
        var request = CreateTemplateRequest.builder()
                .title("测试问卷")
                .schemaDefinition("{}")
                .build();
        var template = QuestionnaireTemplate.builder().id(1L).title("测试问卷").createdBy(1L).build();
        when(templateService.create(request, 1L)).thenReturn(template);

        var response = controller.create(request, mockAuth());

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTitle()).isEqualTo("测试问卷");
        verify(templateService).create(request, 1L);
    }

    @Test
    void update_shouldDelegateToServiceAndReturnSuccess() {
        var request = UpdateTemplateRequest.builder().title("更新标题").schemaDefinition("{}").build();
        var template = QuestionnaireTemplate.builder().id(1L).title("更新标题").createdBy(1L).build();
        when(templateService.update(1L, request)).thenReturn(template);

        var response = controller.update(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTitle()).isEqualTo("更新标题");
    }

    @Test
    void getById_shouldReturnTemplate() {
        var template = QuestionnaireTemplate.builder().id(1L).title("问卷").createdBy(1L).build();
        when(templateService.getById(1L)).thenReturn(template);

        var response = controller.getById(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(1L);
    }

    @Test
    void listAll_shouldReturnAllTemplates() {
        var templates = List.of(
                QuestionnaireTemplate.builder().id(1L).title("问卷1").createdBy(1L).build(),
                QuestionnaireTemplate.builder().id(2L).title("问卷2").createdBy(1L).build()
        );
        when(templateService.listAll()).thenReturn(templates);

        var response = controller.listAll();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    void delete_shouldDelegateToService() {
        doNothing().when(templateService).delete(1L);

        var response = controller.delete(1L);

        assertThat(response.getCode()).isEqualTo(200);
        verify(templateService).delete(1L);
    }

    @Test
    void publish_shouldDelegateToServiceAndReturnVersion() {
        var request = PublishVersionRequest.builder().versionId(5L).build();
        var version = QuestionnaireVersion.builder()
                .id(5L).templateId(1L).versionNumber(1).status(VersionStatus.PUBLISHED).build();
        when(templateService.publish(1L, 5L)).thenReturn(version);

        var response = controller.publish(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getStatus()).isEqualTo(VersionStatus.PUBLISHED);
    }

    @Test
    void getVersionHistory_shouldReturnVersions() {
        var versions = List.of(
                QuestionnaireVersion.builder().id(2L).templateId(1L).versionNumber(2).build(),
                QuestionnaireVersion.builder().id(1L).templateId(1L).versionNumber(1).build()
        );
        when(templateService.getVersionHistory(1L)).thenReturn(versions);

        var response = controller.getVersionHistory(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    void getVersion_shouldReturnSpecificVersion() {
        var version = QuestionnaireVersion.builder()
                .id(5L).templateId(1L).versionNumber(3).schemaDefinition("{}").build();
        when(templateService.getVersion(5L)).thenReturn(version);

        var response = controller.getVersion(5L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getVersionNumber()).isEqualTo(3);
    }
}
