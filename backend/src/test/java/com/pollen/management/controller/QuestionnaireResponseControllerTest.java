package com.pollen.management.controller;

import com.pollen.management.dto.SubmitResponseRequest;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.service.QuestionnaireResponseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireResponseControllerTest {

    @Mock
    private QuestionnaireResponseService responseService;

    @InjectMocks
    private QuestionnaireResponseController controller;

    private Authentication mockAuth() {
        var auth = new UsernamePasswordAuthenticationToken("user", null, List.of());
        auth.setDetails(10L);
        return auth;
    }

    @Test
    void submit_shouldDelegateToServiceWithUserIdFromAuth() {
        var answers = Map.<String, Object>of("name", "李四");
        var request = SubmitResponseRequest.builder().versionId(1L).answers(answers).build();
        var saved = QuestionnaireResponse.builder()
                .id(1L).versionId(1L).userId(10L).answers("{\"name\":\"李四\"}").build();
        when(responseService.submit(1L, 10L, answers)).thenReturn(saved);

        var response = controller.submit(request, mockAuth());

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getUserId()).isEqualTo(10L);
        assertThat(response.getData().getVersionId()).isEqualTo(1L);
    }

    @Test
    void getByApplicationId_shouldReturnResponse() {
        var saved = QuestionnaireResponse.builder()
                .id(1L).versionId(1L).userId(10L).applicationId(5L)
                .answers("{\"name\":\"王五\"}").build();
        when(responseService.getByApplicationId(5L)).thenReturn(saved);

        var response = controller.getByApplicationId(5L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getApplicationId()).isEqualTo(5L);
    }
}
