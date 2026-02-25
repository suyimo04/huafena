package com.pollen.management.service;

import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.repository.PublicLinkRepository;
import com.pollen.management.repository.QuestionnaireTemplateRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicLinkServiceImplTest {

    @Mock
    private PublicLinkRepository publicLinkRepository;

    @Mock
    private QuestionnaireVersionRepository versionRepository;

    @Mock
    private QuestionnaireTemplateRepository templateRepository;

    @Mock
    private QuestionnaireResponseService responseService;

    @InjectMocks
    private PublicLinkServiceImpl publicLinkService;

    @Test
    void getActiveLink_shouldReturnLinkWhenActiveAndNotExpired() {
        var link = PublicLink.builder()
                .id(1L).linkToken("valid-token").templateId(1L).versionId(1L)
                .createdBy(1L).active(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(publicLinkRepository.findByLinkToken("valid-token")).thenReturn(Optional.of(link));

        var result = publicLinkService.getActiveLink("valid-token");

        assertThat(result.getLinkToken()).isEqualTo("valid-token");
    }

    @Test
    void getActiveLink_shouldThrowWhenTokenNotFound() {
        when(publicLinkRepository.findByLinkToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicLinkService.getActiveLink("missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接无效或已过期");
    }

    @Test
    void getActiveLink_shouldThrowWhenLinkInactive() {
        var link = PublicLink.builder()
                .id(1L).linkToken("inactive").templateId(1L).versionId(1L)
                .createdBy(1L).active(false).build();
        when(publicLinkRepository.findByLinkToken("inactive")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> publicLinkService.getActiveLink("inactive"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接无效或已过期");
    }

    @Test
    void getActiveLink_shouldThrowWhenLinkExpired() {
        var link = PublicLink.builder()
                .id(1L).linkToken("expired").templateId(1L).versionId(1L)
                .createdBy(1L).active(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(publicLinkRepository.findByLinkToken("expired")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> publicLinkService.getActiveLink("expired"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接无效或已过期");
    }

    @Test
    void getActiveLink_shouldAllowNullExpiresAt() {
        var link = PublicLink.builder()
                .id(1L).linkToken("no-expiry").templateId(1L).versionId(1L)
                .createdBy(1L).active(true).expiresAt(null).build();
        when(publicLinkRepository.findByLinkToken("no-expiry")).thenReturn(Optional.of(link));

        var result = publicLinkService.getActiveLink("no-expiry");

        assertThat(result.getLinkToken()).isEqualTo("no-expiry");
    }

    @Test
    void getQuestionnaireByToken_shouldReturnVersion() {
        var link = PublicLink.builder()
                .id(1L).linkToken("token").templateId(1L).versionId(5L)
                .createdBy(1L).active(true).build();
        var version = QuestionnaireVersion.builder()
                .id(5L).templateId(1L).versionNumber(1)
                .schemaDefinition("{}").status(VersionStatus.PUBLISHED).build();
        when(publicLinkRepository.findByLinkToken("token")).thenReturn(Optional.of(link));
        when(versionRepository.findById(5L)).thenReturn(Optional.of(version));

        var result = publicLinkService.getQuestionnaireByToken("token");

        assertThat(result.getId()).isEqualTo(5L);
    }

    // --- generate tests ---

    @Test
    void generate_shouldCreateLinkWithUuidToken() {
        var template = QuestionnaireTemplate.builder()
                .id(1L).title("Test").activeVersionId(10L).createdBy(1L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(publicLinkRepository.save(any(PublicLink.class))).thenAnswer(inv -> {
            PublicLink link = inv.getArgument(0);
            link.setId(100L);
            return link;
        });

        PublicLink result = publicLinkService.generate(1L, 5L);

        assertThat(result.getLinkToken()).isNotNull();
        assertThat(result.getLinkToken()).isNotEmpty();
        // UUID format: 8-4-4-4-12
        assertThat(result.getLinkToken()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void generate_shouldAssociateWithActiveVersion() {
        var template = QuestionnaireTemplate.builder()
                .id(1L).title("Test").activeVersionId(10L).createdBy(1L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(publicLinkRepository.save(any(PublicLink.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicLink result = publicLinkService.generate(1L, 5L);

        assertThat(result.getTemplateId()).isEqualTo(1L);
        assertThat(result.getVersionId()).isEqualTo(10L);
        assertThat(result.getCreatedBy()).isEqualTo(5L);
        assertThat(result.getActive()).isTrue();
    }

    @Test
    void generate_shouldThrowWhenTemplateNotFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicLinkService.generate(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("问卷模板不存在");
    }

    @Test
    void generate_shouldThrowWhenTemplateHasNoActiveVersion() {
        var template = QuestionnaireTemplate.builder()
                .id(1L).title("Test").activeVersionId(null).createdBy(1L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> publicLinkService.generate(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("问卷模板尚未发布");
    }

    @Test
    void generate_shouldSaveLinkToRepository() {
        var template = QuestionnaireTemplate.builder()
                .id(1L).title("Test").activeVersionId(10L).createdBy(1L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(publicLinkRepository.save(any(PublicLink.class))).thenAnswer(inv -> inv.getArgument(0));

        publicLinkService.generate(1L, 5L);

        ArgumentCaptor<PublicLink> captor = ArgumentCaptor.forClass(PublicLink.class);
        verify(publicLinkRepository).save(captor.capture());
        PublicLink saved = captor.getValue();
        assertThat(saved.getTemplateId()).isEqualTo(1L);
        assertThat(saved.getVersionId()).isEqualTo(10L);
    }

    @Test
    void generate_shouldProduceUniqueTokensOnMultipleCalls() {
        var template = QuestionnaireTemplate.builder()
                .id(1L).title("Test").activeVersionId(10L).createdBy(1L).build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(publicLinkRepository.save(any(PublicLink.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicLink link1 = publicLinkService.generate(1L, 5L);
        PublicLink link2 = publicLinkService.generate(1L, 5L);

        assertThat(link1.getLinkToken()).isNotEqualTo(link2.getLinkToken());
    }

    // --- listAll tests ---

    @Test
    void listAll_shouldReturnAllPublicLinks() {
        var link1 = PublicLink.builder().id(1L).linkToken("token1").templateId(1L).versionId(1L).createdBy(1L).active(true).build();
        var link2 = PublicLink.builder().id(2L).linkToken("token2").templateId(2L).versionId(2L).createdBy(1L).active(false).build();
        when(publicLinkRepository.findAll()).thenReturn(List.of(link1, link2));

        List<PublicLink> result = publicLinkService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLinkToken()).isEqualTo("token1");
        assertThat(result.get(1).getLinkToken()).isEqualTo("token2");
    }

    @Test
    void listAll_shouldReturnEmptyListWhenNoLinks() {
        when(publicLinkRepository.findAll()).thenReturn(List.of());

        List<PublicLink> result = publicLinkService.listAll();

        assertThat(result).isEmpty();
    }
}
