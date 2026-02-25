package com.pollen.management.controller;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.CreateApplicationRequest;
import com.pollen.management.dto.GeneratePublicLinkRequest;
import com.pollen.management.dto.InitialReviewRequest;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.service.ApplicationService;
import com.pollen.management.service.PublicLinkService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private PublicLinkService publicLinkService;

    @InjectMocks
    private ApplicationController controller;

    private Authentication mockAuth() {
        var auth = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        auth.setDetails(1L);
        return auth;
    }

    // --- POST /api/applications/registration ---

    @Test
    void createFromRegistration_shouldDelegateToServiceAndReturnSuccess() {
        var formData = ApplicationFormData.builder()
                .pollenUid("12345")
                .birthDate(LocalDate.of(2000, 1, 1))
                .educationStage(EducationStage.UNIVERSITY)
                .build();
        var request = CreateApplicationRequest.builder()
                .userId(10L)
                .questionnaireResponseId(20L)
                .formData(formData)
                .build();
        var application = Application.builder()
                .id(1L)
                .userId(10L)
                .status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION)
                .questionnaireResponseId(20L)
                .build();
        when(applicationService.createFromRegistration(10L, 20L, formData)).thenReturn(application);

        var response = controller.createFromRegistration(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getUserId()).isEqualTo(10L);
        assertThat(response.getData().getStatus()).isEqualTo(ApplicationStatus.PENDING_INITIAL_REVIEW);
        assertThat(response.getData().getEntryType()).isEqualTo(EntryType.REGISTRATION);
        assertThat(response.getData().getQuestionnaireResponseId()).isEqualTo(20L);
        verify(applicationService).createFromRegistration(10L, 20L, formData);
    }

    @Test
    void createFromRegistration_duplicateApplication_shouldPropagateException() {
        var formData = ApplicationFormData.builder()
                .pollenUid("12345")
                .birthDate(LocalDate.of(2000, 1, 1))
                .educationStage(EducationStage.UNIVERSITY)
                .build();
        var request = CreateApplicationRequest.builder()
                .userId(10L)
                .questionnaireResponseId(20L)
                .formData(formData)
                .build();
        when(applicationService.createFromRegistration(10L, 20L, formData))
                .thenThrow(new BusinessException(409, "已有未处理的申请"));

        assertThatThrownBy(() -> controller.createFromRegistration(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已有未处理的申请");
    }

    // --- GET /api/applications ---

    @Test
    void listAll_shouldReturnApplicationsOrderedByCreatedAtDesc() {
        var now = LocalDateTime.now();
        var app1 = Application.builder()
                .id(2L).userId(10L).status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                .entryType(EntryType.REGISTRATION).build();
        var app2 = Application.builder()
                .id(1L).userId(11L).status(ApplicationStatus.REJECTED)
                .entryType(EntryType.PUBLIC_LINK).build();
        when(applicationService.listAll()).thenReturn(List.of(app1, app2));

        var response = controller.listAll();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getId()).isEqualTo(2L);
        assertThat(response.getData().get(1).getId()).isEqualTo(1L);
        verify(applicationService).listAll();
    }

    @Test
    void listAll_emptyList_shouldReturnEmptySuccess() {
        when(applicationService.listAll()).thenReturn(List.of());

        var response = controller.listAll();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- POST /api/applications/{id}/initial-review ---

    @Test
    void initialReview_approve_shouldDelegateToService() {
        var request = InitialReviewRequest.builder().approved(true).build();
        doNothing().when(applicationService).initialReview(1L, true);

        var response = controller.initialReview(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(applicationService).initialReview(1L, true);
    }

    @Test
    void initialReview_reject_shouldDelegateToService() {
        var request = InitialReviewRequest.builder().approved(false).build();
        doNothing().when(applicationService).initialReview(1L, false);

        var response = controller.initialReview(1L, request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(applicationService).initialReview(1L, false);
    }

    @Test
    void initialReview_applicationNotFound_shouldPropagateException() {
        var request = InitialReviewRequest.builder().approved(true).build();
        doThrow(new BusinessException(404, "申请记录不存在"))
                .when(applicationService).initialReview(99L, true);

        assertThatThrownBy(() -> controller.initialReview(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("申请记录不存在");
    }

    @Test
    void initialReview_invalidStatus_shouldPropagateException() {
        var request = InitialReviewRequest.builder().approved(true).build();
        doThrow(new BusinessException(400, "当前申请状态不允许此操作"))
                .when(applicationService).initialReview(1L, true);

        assertThatThrownBy(() -> controller.initialReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前申请状态不允许此操作");
    }

    // --- POST /api/applications/public-links/generate ---

    @Test
    void generatePublicLink_shouldDelegateToServiceAndReturnSuccess() {
        var request = GeneratePublicLinkRequest.builder().templateId(5L).build();
        var link = PublicLink.builder()
                .id(1L)
                .linkToken("uuid-token-123")
                .templateId(5L)
                .versionId(10L)
                .createdBy(1L)
                .active(true)
                .build();
        when(publicLinkService.generate(5L, 1L)).thenReturn(link);

        var response = controller.generatePublicLink(request, mockAuth());

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getLinkToken()).isEqualTo("uuid-token-123");
        assertThat(response.getData().getTemplateId()).isEqualTo(5L);
        assertThat(response.getData().getActive()).isTrue();
        verify(publicLinkService).generate(5L, 1L);
    }

    @Test
    void generatePublicLink_templateNotFound_shouldPropagateException() {
        var request = GeneratePublicLinkRequest.builder().templateId(99L).build();
        when(publicLinkService.generate(99L, 1L))
                .thenThrow(new BusinessException(404, "问卷模板不存在"));

        assertThatThrownBy(() -> controller.generatePublicLink(request, mockAuth()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("问卷模板不存在");
    }

    @Test
    void generatePublicLink_templateNotPublished_shouldPropagateException() {
        var request = GeneratePublicLinkRequest.builder().templateId(5L).build();
        when(publicLinkService.generate(5L, 1L))
                .thenThrow(new BusinessException(400, "问卷模板尚未发布"));

        assertThatThrownBy(() -> controller.generatePublicLink(request, mockAuth()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("问卷模板尚未发布");
    }

    // --- GET /api/applications/public-links ---

    @Test
    void listPublicLinks_shouldReturnAllLinks() {
        var links = List.of(
                PublicLink.builder().id(1L).linkToken("token-1").templateId(5L)
                        .versionId(10L).createdBy(1L).active(true).build(),
                PublicLink.builder().id(2L).linkToken("token-2").templateId(6L)
                        .versionId(11L).createdBy(1L).active(false).build()
        );
        when(publicLinkService.listAll()).thenReturn(links);

        var response = controller.listPublicLinks();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getLinkToken()).isEqualTo("token-1");
        assertThat(response.getData().get(1).getLinkToken()).isEqualTo("token-2");
        verify(publicLinkService).listAll();
    }

    @Test
    void listPublicLinks_emptyList_shouldReturnEmptySuccess() {
        when(publicLinkService.listAll()).thenReturn(List.of());

        var response = controller.listPublicLinks();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }
}
