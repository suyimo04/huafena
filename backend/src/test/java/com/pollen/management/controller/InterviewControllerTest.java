package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.Interview;
import com.pollen.management.entity.InterviewMessage;
import com.pollen.management.entity.InterviewReport;
import com.pollen.management.entity.enums.InterviewStatus;
import com.pollen.management.service.InterviewService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock
    private InterviewService interviewService;

    @InjectMocks
    private InterviewController controller;

    // --- POST /api/interviews/start ---

    @Test
    void startInterview_shouldDelegateToServiceAndReturnSuccess() {
        var request = StartInterviewRequest.builder()
                .applicationId(1L)
                .scenarioId("conflict_handling")
                .build();
        var interview = Interview.builder()
                .id(10L)
                .applicationId(1L)
                .userId(5L)
                .status(InterviewStatus.IN_PROGRESS)
                .scenarioId("conflict_handling")
                .difficultyLevel("standard")
                .build();
        when(interviewService.startInterview(1L, "conflict_handling")).thenReturn(interview);

        var response = controller.startInterview(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(10L);
        assertThat(response.getData().getApplicationId()).isEqualTo(1L);
        assertThat(response.getData().getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(response.getData().getScenarioId()).isEqualTo("conflict_handling");
        verify(interviewService).startInterview(1L, "conflict_handling");
    }

    @Test
    void startInterview_applicationNotFound_shouldPropagateException() {
        var request = StartInterviewRequest.builder()
                .applicationId(99L)
                .scenarioId("conflict_handling")
                .build();
        when(interviewService.startInterview(99L, "conflict_handling"))
                .thenThrow(new BusinessException(404, "申请记录不存在"));

        assertThatThrownBy(() -> controller.startInterview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("申请记录不存在");
    }

    // --- POST /api/interviews/{id}/message ---

    @Test
    void processMessage_shouldDelegateToServiceAndReturnAiReply() {
        var request = InterviewMessageRequest.builder()
                .message("我会先了解情况再处理")
                .build();
        var aiMessage = InterviewMessage.builder()
                .id(20L)
                .interviewId(10L)
                .role("AI")
                .content("好的，那如果对方情绪激动怎么办？")
                .timestamp(LocalDateTime.now())
                .build();
        when(interviewService.processMessage(10L, "我会先了解情况再处理")).thenReturn(aiMessage);

        var response = controller.processMessage(10L, request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getRole()).isEqualTo("AI");
        assertThat(response.getData().getInterviewId()).isEqualTo(10L);
        verify(interviewService).processMessage(10L, "我会先了解情况再处理");
    }

    @Test
    void processMessage_interviewNotFound_shouldPropagateException() {
        var request = InterviewMessageRequest.builder().message("test").build();
        when(interviewService.processMessage(99L, "test"))
                .thenThrow(new BusinessException(404, "面试记录不存在"));

        assertThatThrownBy(() -> controller.processMessage(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("面试记录不存在");
    }

    // --- POST /api/interviews/{id}/end ---

    @Test
    void endInterview_shouldDelegateToServiceAndReturnReport() {
        var report = InterviewReport.builder()
                .id(30L)
                .interviewId(10L)
                .ruleFamiliarity(8)
                .communicationScore(7)
                .pressureScore(6)
                .totalScore(7)
                .aiComment("表现良好")
                .recommendationLabel("重点审查对话内容")
                .build();
        when(interviewService.endInterview(10L)).thenReturn(report);

        var response = controller.endInterview(10L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTotalScore()).isEqualTo(7);
        assertThat(response.getData().getRecommendationLabel()).isEqualTo("重点审查对话内容");
        verify(interviewService).endInterview(10L);
    }

    @Test
    void endInterview_interviewNotFound_shouldPropagateException() {
        when(interviewService.endInterview(99L))
                .thenThrow(new BusinessException(404, "面试记录不存在"));

        assertThatThrownBy(() -> controller.endInterview(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("面试记录不存在");
    }

    // --- GET /api/interviews/{id} ---

    @Test
    void getInterview_shouldReturnInterviewDetails() {
        var interview = Interview.builder()
                .id(10L)
                .applicationId(1L)
                .userId(5L)
                .status(InterviewStatus.COMPLETED)
                .scenarioId("violation_judgment")
                .build();
        when(interviewService.getInterview(10L)).thenReturn(interview);

        var response = controller.getInterview(10L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getId()).isEqualTo(10L);
        assertThat(response.getData().getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        verify(interviewService).getInterview(10L);
    }

    @Test
    void getInterview_notFound_shouldPropagateException() {
        when(interviewService.getInterview(99L))
                .thenThrow(new BusinessException(404, "面试记录不存在"));

        assertThatThrownBy(() -> controller.getInterview(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("面试记录不存在");
    }

    // --- GET /api/interviews/{id}/messages ---

    @Test
    void getMessages_shouldReturnMessageList() {
        var messages = List.of(
                InterviewMessage.builder().id(1L).interviewId(10L).role("AI")
                        .content("你好").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().id(2L).interviewId(10L).role("USER")
                        .content("你好").timestamp(LocalDateTime.now()).build()
        );
        when(interviewService.getMessages(10L)).thenReturn(messages);

        var response = controller.getMessages(10L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getRole()).isEqualTo("AI");
        assertThat(response.getData().get(1).getRole()).isEqualTo("USER");
        verify(interviewService).getMessages(10L);
    }

    @Test
    void getMessages_emptyList_shouldReturnEmptySuccess() {
        when(interviewService.getMessages(10L)).thenReturn(List.of());

        var response = controller.getMessages(10L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- GET /api/interviews/{id}/report ---

    @Test
    void getReport_shouldReturnInterviewReport() {
        var report = InterviewReport.builder()
                .id(30L)
                .interviewId(10L)
                .ruleFamiliarity(9)
                .communicationScore(8)
                .pressureScore(8)
                .totalScore(8)
                .aiComment("优秀")
                .recommendationLabel("建议通过")
                .build();
        when(interviewService.getReport(10L)).thenReturn(report);

        var response = controller.getReport(10L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getTotalScore()).isEqualTo(8);
        assertThat(response.getData().getRecommendationLabel()).isEqualTo("建议通过");
        verify(interviewService).getReport(10L);
    }

    @Test
    void getReport_notFound_shouldPropagateException() {
        when(interviewService.getReport(99L))
                .thenThrow(new BusinessException(404, "面试报告不存在"));

        assertThatThrownBy(() -> controller.getReport(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("面试报告不存在");
    }

    // --- POST /api/interviews/{id}/review ---

    @Test
    void manualReview_approve_shouldDelegateToServiceAndReturnReport() {
        var request = ManualReviewRequest.builder()
                .approved(true)
                .reviewComment("表现优秀，建议录用")
                .suggestedMentor("leader")
                .build();
        var report = InterviewReport.builder()
                .id(30L)
                .interviewId(10L)
                .totalScore(8)
                .manualApproved(true)
                .reviewerComment("表现优秀，建议录用")
                .suggestedMentor("leader")
                .reviewResult("通过")
                .build();
        when(interviewService.manualReview(10L, true, "表现优秀，建议录用", "leader", null))
                .thenReturn(report);

        var response = controller.manualReview(10L, request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getManualApproved()).isTrue();
        assertThat(response.getData().getReviewResult()).isEqualTo("通过");
        assertThat(response.getData().getSuggestedMentor()).isEqualTo("leader");
        verify(interviewService).manualReview(10L, true, "表现优秀，建议录用", "leader", null);
    }

    @Test
    void manualReview_reject_shouldDelegateToServiceAndReturnReport() {
        var request = ManualReviewRequest.builder()
                .approved(false)
                .reviewComment("沟通能力不足")
                .build();
        var report = InterviewReport.builder()
                .id(30L)
                .interviewId(10L)
                .totalScore(5)
                .manualApproved(false)
                .reviewerComment("沟通能力不足")
                .reviewResult("拒绝")
                .build();
        when(interviewService.manualReview(10L, false, "沟通能力不足", null, null))
                .thenReturn(report);

        var response = controller.manualReview(10L, request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getManualApproved()).isFalse();
        assertThat(response.getData().getReviewResult()).isEqualTo("拒绝");
        verify(interviewService).manualReview(10L, false, "沟通能力不足", null, null);
    }

    @Test
    void manualReview_interviewNotCompleted_shouldPropagateException() {
        var request = ManualReviewRequest.builder().approved(true).build();
        when(interviewService.manualReview(10L, true, null, null, null))
                .thenThrow(new BusinessException(400, "面试尚未完成，无法进行人工复审"));

        assertThatThrownBy(() -> controller.manualReview(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("面试尚未完成，无法进行人工复审");
    }

    // --- GET /api/interviews/{id}/archive ---

    @Test
    void getArchive_shouldReturnFullArchivedRecord() {
        var interview = Interview.builder()
                .id(10L).applicationId(1L).userId(5L)
                .status(InterviewStatus.REVIEWED).build();
        var messages = List.of(
                InterviewMessage.builder().id(1L).interviewId(10L).role("AI")
                        .content("场景开始").timestamp(LocalDateTime.now()).build()
        );
        var report = InterviewReport.builder()
                .id(30L).interviewId(10L).totalScore(8)
                .manualApproved(true).reviewResult("通过").build();
        var archive = InterviewArchiveRecord.builder()
                .interview(interview)
                .messages(messages)
                .report(report)
                .build();
        when(interviewService.getFullArchivedRecord(10L)).thenReturn(archive);

        var response = controller.getArchive(10L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getInterview().getId()).isEqualTo(10L);
        assertThat(response.getData().getMessages()).hasSize(1);
        assertThat(response.getData().getReport().getTotalScore()).isEqualTo(8);
        verify(interviewService).getFullArchivedRecord(10L);
    }

    @Test
    void getArchive_notFound_shouldPropagateException() {
        when(interviewService.getFullArchivedRecord(99L))
                .thenThrow(new BusinessException(404, "面试记录不存在"));

        assertThatThrownBy(() -> controller.getArchive(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("面试记录不存在");
    }
}
