package com.pollen.management.controller;

import com.pollen.management.dto.PublicSubmitRequest;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.entity.enums.VersionStatus;
import com.pollen.management.service.PublicLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicQuestionnaireControllerTest {

    @Mock
    private PublicLinkService publicLinkService;

    @InjectMocks
    private PublicQuestionnaireController controller;

    @Test
    void getByToken_shouldReturnQuestionnaireVersion() {
        var version = QuestionnaireVersion.builder()
                .id(1L).templateId(1L).versionNumber(1)
                .schemaDefinition("{\"fields\":[]}")
                .status(VersionStatus.PUBLISHED)
                .build();
        when(publicLinkService.getQuestionnaireByToken("abc-token")).thenReturn(version);

        var response = controller.getByToken("abc-token");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getSchemaDefinition()).contains("fields");
    }

    @Test
    void submitByToken_shouldReturnSuccessResult() {
        var answers = Map.<String, Object>of("name", "张三", "age", 25);
        var request = PublicSubmitRequest.builder().answers(answers).build();
        var result = Map.<String, Object>of("message", "问卷提交成功", "linkToken", "abc-token");
        when(publicLinkService.submitByToken("abc-token", answers)).thenReturn(result);

        var response = controller.submitByToken("abc-token", request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsEntry("message", "问卷提交成功");
    }
}
