package com.pollen.management.service;

import com.pollen.management.dto.InterviewArchiveRecord;
import com.pollen.management.dto.InterviewScenario;
import com.pollen.management.entity.*;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.InterviewStatus;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.*;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InterviewServiceImpl 单元测试
 * Validates: Requirements 5.3, 5.4, 5.5, 5.6, 5.7, 5.11, 5.12, 5.13
 */
@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private InterviewMessageRepository messageRepository;
    @Mock
    private InterviewReportRepository reportRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private InterviewScenarioService scenarioService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InternshipService internshipService;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    private Application testApplication;
    private InterviewScenario testScenario;
    private Interview testInterview;

    @BeforeEach
    void setUp() {
        testApplication = Application.builder()
                .id(1L)
                .userId(10L)
                .status(ApplicationStatus.INITIAL_REVIEW_PASSED)
                .entryType(EntryType.REGISTRATION)
                .build();

        testScenario = InterviewScenario.builder()
                .id("conflict-resolution-1")
                .name("群内成员争执调解")
                .description("测试场景")
                .difficulty(1)
                .category("冲突处理")
                .studentOnly(false)
                .aiRole("争吵的群成员")
                .initialPrompt("你好，我是群成员小明。刚才在群里和另一个人吵起来了。")
                .build();

        testInterview = Interview.builder()
                .id(100L)
                .applicationId(1L)
                .userId(10L)
                .scenarioId("conflict-resolution-1")
                .status(InterviewStatus.IN_PROGRESS)
                .difficultyLevel("1")
                .build();
    }

    // --- startInterview tests ---

    @Test
    void startInterview_shouldCreateInterviewWithInProgressStatus() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(interviewRepository.findByApplicationId(1L)).thenReturn(Optional.empty());
        when(scenarioService.getScenarios(true)).thenReturn(List.of(testScenario));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = interviewService.startInterview(1L, "conflict-resolution-1");

        assertEquals(InterviewStatus.IN_PROGRESS, result.getStatus());
        assertEquals(1L, result.getApplicationId());
        assertEquals(10L, result.getUserId());
        assertEquals("conflict-resolution-1", result.getScenarioId());
    }

    @Test
    void startInterview_shouldSendInitialAiMessage() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(interviewRepository.findByApplicationId(1L)).thenReturn(Optional.empty());
        when(scenarioService.getScenarios(true)).thenReturn(List.of(testScenario));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.startInterview(1L, "conflict-resolution-1");

        ArgumentCaptor<InterviewMessage> msgCaptor = ArgumentCaptor.forClass(InterviewMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        InterviewMessage initialMsg = msgCaptor.getValue();
        assertEquals("AI", initialMsg.getRole());
        assertEquals(testScenario.getInitialPrompt(), initialMsg.getContent());
        assertEquals(60, initialMsg.getTimeLimitSeconds());
    }

    @Test
    void startInterview_shouldUpdateApplicationStatusToAiInterviewInProgress() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(interviewRepository.findByApplicationId(1L)).thenReturn(Optional.empty());
        when(scenarioService.getScenarios(true)).thenReturn(List.of(testScenario));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.startInterview(1L, "conflict-resolution-1");

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertEquals(ApplicationStatus.AI_INTERVIEW_IN_PROGRESS, appCaptor.getValue().getStatus());
    }

    @Test
    void startInterview_applicationNotFound_shouldThrow404() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.startInterview(999L, "conflict-resolution-1"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void startInterview_existingInProgressInterview_shouldThrow400() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        Interview existing = Interview.builder().status(InterviewStatus.IN_PROGRESS).build();
        when(interviewRepository.findByApplicationId(1L)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.startInterview(1L, "conflict-resolution-1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已有进行中的面试"));
    }

    @Test
    void startInterview_scenarioNotFound_shouldThrow404() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(interviewRepository.findByApplicationId(1L)).thenReturn(Optional.empty());
        when(scenarioService.getScenarios(true)).thenReturn(List.of(testScenario));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.startInterview(1L, "nonexistent-scenario"));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("面试场景不存在"));
    }

    // --- processMessage tests ---

    @Test
    void processMessage_shouldRecordUserMessageAndAiResponse() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewMessage aiResponse = interviewService.processMessage(100L, "我会先了解情况，保持冷静沟通");

        ArgumentCaptor<InterviewMessage> captor = ArgumentCaptor.forClass(InterviewMessage.class);
        verify(messageRepository, times(2)).save(captor.capture());

        List<InterviewMessage> savedMessages = captor.getAllValues();
        // First saved message is user message
        assertEquals("USER", savedMessages.get(0).getRole());
        assertEquals("我会先了解情况，保持冷静沟通", savedMessages.get(0).getContent());
        assertEquals(60, savedMessages.get(0).getTimeLimitSeconds());
        // Second saved message is AI response
        assertEquals("AI", savedMessages.get(1).getRole());
        assertNotNull(savedMessages.get(1).getContent());
        assertEquals(60, savedMessages.get(1).getTimeLimitSeconds());
    }

    @Test
    void processMessage_shouldReturnAiMessage() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewMessage result = interviewService.processMessage(100L, "我理解你的感受");

        assertEquals("AI", result.getRole());
        assertNotNull(result.getContent());
        assertFalse(result.getContent().isEmpty());
    }

    @Test
    void processMessage_interviewNotFound_shouldThrow404() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.processMessage(999L, "test"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void processMessage_interviewNotInProgress_shouldThrow400() {
        Interview completed = Interview.builder()
                .id(100L).status(InterviewStatus.COMPLETED).build();
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completed));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.processMessage(100L, "test"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不在进行中状态"));
    }

    @Test
    void processMessage_conflictScenario_shouldGenerateContextualResponse() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewMessage result = interviewService.processMessage(100L, "我会先让双方冷静下来");

        assertNotNull(result.getContent());
        // Conflict scenario with "冷静" keyword should trigger specific response
        assertTrue(result.getContent().contains("道理") || result.getContent().contains("公平"));
    }

    @Test
    void processMessage_violationScenario_shouldGenerateContextualResponse() {
        Interview violationInterview = Interview.builder()
                .id(101L).scenarioId("violation-judgment-1").status(InterviewStatus.IN_PROGRESS).build();
        when(interviewRepository.findById(101L)).thenReturn(Optional.of(violationInterview));
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewMessage result = interviewService.processMessage(101L, "让我先查看一下证据");

        assertNotNull(result.getContent());
    }

    // --- endInterview tests ---

    @Test
    void endInterview_shouldGenerateReportAndUpdateStatuses() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().role("AI").content("初始问题").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER").content("我会根据群规来处理，先了解情况再做判断").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("AI").content("AI回复").timestamp(LocalDateTime.now()).build()
        );
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(messages);
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> {
            InterviewReport r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        InterviewReport report = interviewService.endInterview(100L);

        // Report should have valid scores
        assertNotNull(report);
        assertTrue(report.getRuleFamiliarity() >= 0 && report.getRuleFamiliarity() <= 10);
        assertTrue(report.getCommunicationScore() >= 0 && report.getCommunicationScore() <= 10);
        assertTrue(report.getPressureScore() >= 0 && report.getPressureScore() <= 10);
        assertTrue(report.getTotalScore() >= 0 && report.getTotalScore() <= 10);
        assertNotNull(report.getAiComment());
    }

    @Test
    void endInterview_shouldSetInterviewStatusToCompleted() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(Collections.emptyList());
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        interviewService.endInterview(100L);

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(interviewRepository).save(captor.capture());
        assertEquals(InterviewStatus.COMPLETED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getCompletedAt());
    }

    @Test
    void endInterview_shouldUpdateApplicationStatusToPendingReview() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(Collections.emptyList());
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        interviewService.endInterview(100L);

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        assertEquals(ApplicationStatus.PENDING_REVIEW, captor.getValue().getStatus());
    }

    @Test
    void endInterview_totalScoreIsAverageOfThreeScores() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().role("USER").content("根据群规规定，这属于违规行为").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER").content("我理解你的感受，让我们冷静沟通").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER").content("我有一个解决方案可以处理这个问题，请耐心听我说完").timestamp(LocalDateTime.now()).build()
        );
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(messages);
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        InterviewReport report = interviewService.endInterview(100L);

        int expectedTotal = Math.round((report.getRuleFamiliarity() + report.getCommunicationScore() + report.getPressureScore()) / 3.0f);
        expectedTotal = Math.max(0, Math.min(10, expectedTotal));
        assertEquals(expectedTotal, report.getTotalScore());
    }

    @Test
    void endInterview_interviewNotFound_shouldThrow404() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.endInterview(999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void endInterview_interviewNotInProgress_shouldThrow400() {
        Interview completed = Interview.builder()
                .id(100L).status(InterviewStatus.COMPLETED).build();
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completed));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.endInterview(100L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void endInterview_withNoMessages_shouldStillGenerateReport() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(Collections.emptyList());
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        InterviewReport report = interviewService.endInterview(100L);

        assertNotNull(report);
        assertTrue(report.getRuleFamiliarity() >= 0 && report.getRuleFamiliarity() <= 10);
        assertTrue(report.getTotalScore() >= 0 && report.getTotalScore() <= 10);
    }

    // --- getInterview tests ---

    @Test
    void getInterview_shouldReturnInterview() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));

        Interview result = interviewService.getInterview(100L);

        assertEquals(100L, result.getId());
        assertEquals(InterviewStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void getInterview_notFound_shouldThrow404() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.getInterview(999L));
        assertEquals(404, ex.getCode());
    }

    // --- getMessages tests ---

    @Test
    void getMessages_shouldReturnOrderedMessages() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().role("AI").content("问题1").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER").content("回答1").timestamp(LocalDateTime.now()).build()
        );
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(messages);

        List<InterviewMessage> result = interviewService.getMessages(100L);

        assertEquals(2, result.size());
        assertEquals("AI", result.get(0).getRole());
        assertEquals("USER", result.get(1).getRole());
    }

    @Test
    void getMessages_interviewNotFound_shouldThrow404() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.getMessages(999L));
        assertEquals(404, ex.getCode());
    }

    // --- getReport tests ---

    @Test
    void getReport_shouldReturnReport() {
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).ruleFamiliarity(7)
                .communicationScore(8).pressureScore(6).totalScore(7)
                .aiComment("评语").build();
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));

        InterviewReport result = interviewService.getReport(100L);

        assertEquals(100L, result.getInterviewId());
        assertEquals(7, result.getRuleFamiliarity());
    }

    @Test
    void getReport_notFound_shouldThrow404() {
        when(reportRepository.findByInterviewId(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.getReport(999L));
        assertEquals(404, ex.getCode());
    }

    // --- AI response generation tests ---

    @Test
    void generateSimulatedResponse_defaultScenario_shouldReturnGenericResponse() {
        Interview unknownScenario = Interview.builder()
                .id(100L).scenarioId("unknown-scenario").status(InterviewStatus.IN_PROGRESS).build();

        String response = interviewService.generateSimulatedResponse(unknownScenario, "test message");

        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void generateSimulatedResponse_complaintScenario_shouldReturnContextualResponse() {
        Interview complaintInterview = Interview.builder()
                .id(100L).scenarioId("complaint-handling-1").status(InterviewStatus.IN_PROGRESS).build();

        String response = interviewService.generateSimulatedResponse(complaintInterview, "我们会改进服务");

        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void generateSimulatedResponse_studyWorkScenario_shouldReturnContextualResponse() {
        Interview studyInterview = Interview.builder()
                .id(100L).scenarioId("study-work-balance-1").status(InterviewStatus.IN_PROGRESS).build();

        String response = interviewService.generateSimulatedResponse(studyInterview, "我会合理安排时间");

        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    // --- Report generation tests ---

    @Test
    void generateEvaluationReport_scoresAreClamped0To10() {
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().role("USER")
                        .content("群规 规则 规定 违规 处罚 警告 理解 沟通 倾听 抱歉 对不起 同理 冷静 耐心 理性 方案 解决 处理")
                        .timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER")
                        .content("群规 规则 规定 违规 处罚 警告 理解 沟通 倾听 抱歉 对不起 同理 冷静 耐心 理性 方案 解决 处理")
                        .timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER")
                        .content("群规 规则 规定 违规 处罚 警告 理解 沟通 倾听 抱歉 对不起 同理 冷静 耐心 理性 方案 解决 处理")
                        .timestamp(LocalDateTime.now()).build()
        );

        InterviewReport report = interviewService.generateEvaluationReport(100L, messages);

        assertTrue(report.getRuleFamiliarity() >= 0 && report.getRuleFamiliarity() <= 10);
        assertTrue(report.getCommunicationScore() >= 0 && report.getCommunicationScore() <= 10);
        assertTrue(report.getPressureScore() >= 0 && report.getPressureScore() <= 10);
        assertTrue(report.getTotalScore() >= 0 && report.getTotalScore() <= 10);
    }

    @Test
    void generateEvaluationReport_aiCommentContainsRoundCount() {
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().role("USER").content("回答1").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().role("USER").content("回答2").timestamp(LocalDateTime.now()).build()
        );

        InterviewReport report = interviewService.generateEvaluationReport(100L, messages);

        assertTrue(report.getAiComment().contains("2轮对话"));
    }

    // --- getRecommendationLabel tests ---
    // Validates: Requirements 5.8, 5.9, 5.10

    @Test
    void getRecommendationLabel_score10_shouldReturnSuggestPass() {
        assertEquals("建议通过", interviewService.getRecommendationLabel(10));
    }

    @Test
    void getRecommendationLabel_score9_shouldReturnSuggestPass() {
        assertEquals("建议通过", interviewService.getRecommendationLabel(9));
    }

    @Test
    void getRecommendationLabel_score8_shouldReturnSuggestPass() {
        assertEquals("建议通过", interviewService.getRecommendationLabel(8));
    }

    @Test
    void getRecommendationLabel_score7_shouldReturnReviewDialogue() {
        assertEquals("重点审查对话内容", interviewService.getRecommendationLabel(7));
    }

    @Test
    void getRecommendationLabel_score6_shouldReturnReviewDialogue() {
        assertEquals("重点审查对话内容", interviewService.getRecommendationLabel(6));
    }

    @Test
    void getRecommendationLabel_score5_shouldReturnSuggestReject() {
        assertEquals("建议拒绝", interviewService.getRecommendationLabel(5));
    }

    @Test
    void getRecommendationLabel_score0_shouldReturnSuggestReject() {
        assertEquals("建议拒绝", interviewService.getRecommendationLabel(0));
    }

    @Test
    void getRecommendationLabel_score3_shouldReturnSuggestReject() {
        assertEquals("建议拒绝", interviewService.getRecommendationLabel(3));
    }

    @Test
    void endInterview_shouldSetRecommendationLabelOnReport() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(Collections.emptyList());
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        InterviewReport report = interviewService.endInterview(100L);

        assertNotNull(report.getRecommendationLabel());
        String label = report.getRecommendationLabel();
        assertTrue(
            label.equals("建议通过") || label.equals("重点审查对话内容") || label.equals("建议拒绝"),
            "Label should be one of the three valid values, got: " + label
        );
    }

    @Test
    void processMessage_eachMessageHas60SecondTimeLimit() {
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(messageRepository.save(any(InterviewMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.processMessage(100L, "测试消息");

        ArgumentCaptor<InterviewMessage> captor = ArgumentCaptor.forClass(InterviewMessage.class);
        verify(messageRepository, times(2)).save(captor.capture());
        for (InterviewMessage msg : captor.getAllValues()) {
            assertEquals(60, msg.getTimeLimitSeconds(), "Each message should have 60 second time limit");
        }
    }

    // --- manualReview tests ---
    // Validates: Requirements 5.7, 5.11, 5.12, 5.13

    @Test
    void manualReview_approved_shouldUpdateApplicationToInternOffered() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(7).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.manualReview(100L, true, "表现优秀", "导师张三", null);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertEquals(ApplicationStatus.INTERN_OFFERED, appCaptor.getValue().getStatus());
    }

    @Test
    void manualReview_approved_shouldChangeUserRoleToIntern() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(7).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.manualReview(100L, true, "表现优秀", "导师张三", null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.INTERN, userCaptor.getValue().getRole());
        assertTrue(userCaptor.getValue().getEnabled());
    }

    @Test
    void manualReview_approved_shouldStoreReviewDetailsOnReport() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(7).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewReport result = interviewService.manualReview(100L, true, "表现优秀", "导师张三", null);

        assertTrue(result.getManualApproved());
        assertEquals("表现优秀", result.getReviewerComment());
        assertEquals("导师张三", result.getSuggestedMentor());
        assertEquals("通过", result.getReviewResult());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void manualReview_rejected_shouldUpdateApplicationToRejected() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(3).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.manualReview(100L, false, "规则熟悉度不足", null, null);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertEquals(ApplicationStatus.REJECTED, appCaptor.getValue().getStatus());
    }

    @Test
    void manualReview_rejected_shouldStoreRejectionOnReport() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(3).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewReport result = interviewService.manualReview(100L, false, "规则熟悉度不足", null, null);

        assertFalse(result.getManualApproved());
        assertEquals("规则熟悉度不足", result.getReviewerComment());
        assertEquals("拒绝", result.getReviewResult());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void manualReview_shouldSetInterviewStatusToReviewed() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(7).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.manualReview(100L, true, "通过", null, null);

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(interviewRepository).save(captor.capture());
        assertEquals(InterviewStatus.REVIEWED, captor.getValue().getStatus());
    }

    @Test
    void manualReview_interviewNotFound_shouldThrow404() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.manualReview(999L, true, "通过", null, null));
        assertEquals(404, ex.getCode());
    }

    @Test
    void manualReview_interviewNotCompleted_shouldThrow400() {
        Interview inProgress = Interview.builder()
                .id(100L).status(InterviewStatus.IN_PROGRESS).build();
        when(interviewRepository.findById(100L)).thenReturn(Optional.of(inProgress));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.manualReview(100L, true, "通过", null, null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("面试尚未完成"));
    }

    @Test
    void manualReview_overridesAiRecommendation_humanApprovesDespiteLowScore() {
        // AI scored 3 (建议拒绝) but human approves - human judgment takes precedence
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(3)
                .recommendationLabel("建议拒绝").build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewReport result = interviewService.manualReview(100L, true, "虽然AI评分低，但面试表现实际不错", "导师李四", null);

        // Human judgment overrides AI: approved despite low AI score
        assertTrue(result.getManualApproved());
        assertEquals("通过", result.getReviewResult());

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertEquals(ApplicationStatus.INTERN_OFFERED, appCaptor.getValue().getStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.INTERN, userCaptor.getValue().getRole());
    }

    @Test
    void manualReview_overridesAiRecommendation_humanRejectsDespiteHighScore() {
        // AI scored 9 (建议通过) but human rejects - human judgment takes precedence
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(9)
                .recommendationLabel("建议通过").build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewReport result = interviewService.manualReview(100L, false, "虽然AI评分高，但发现态度问题", null, null);

        // Human judgment overrides AI: rejected despite high AI score
        assertFalse(result.getManualApproved());
        assertEquals("拒绝", result.getReviewResult());

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertEquals(ApplicationStatus.REJECTED, appCaptor.getValue().getStatus());
    }

    // --- getFullArchivedRecord tests ---
    // Validates: Requirements 5.14

    @Test
    void getFullArchivedRecord_shouldReturnCompleteArchive() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.REVIEWED).build();
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().id(1L).interviewId(100L).role("AI").content("初始问题").timestamp(LocalDateTime.now()).build(),
                InterviewMessage.builder().id(2L).interviewId(100L).role("USER").content("用户回答").timestamp(LocalDateTime.now()).build()
        );
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).ruleFamiliarity(7).communicationScore(8)
                .pressureScore(6).totalScore(7).aiComment("评语")
                .manualApproved(true).reviewerComment("表现优秀")
                .suggestedMentor("导师张三").reviewResult("通过")
                .recommendationLabel("重点审查对话内容")
                .reviewedAt(LocalDateTime.now()).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(messages);
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));

        InterviewArchiveRecord archive = interviewService.getFullArchivedRecord(100L);

        assertNotNull(archive);
        assertEquals(completedInterview, archive.getInterview());
        assertEquals(2, archive.getMessages().size());
        assertEquals("AI", archive.getMessages().get(0).getRole());
        assertEquals("USER", archive.getMessages().get(1).getRole());
        assertNotNull(archive.getReport());
        assertEquals(7, archive.getReport().getTotalScore());
        assertTrue(archive.getReport().getManualApproved());
        assertEquals("表现优秀", archive.getReport().getReviewerComment());
        assertEquals("导师张三", archive.getReport().getSuggestedMentor());
    }

    @Test
    void getFullArchivedRecord_interviewNotFound_shouldThrow404() {
        when(interviewRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.getFullArchivedRecord(999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getFullArchivedRecord_withNoReport_shouldReturnNullReport() {
        Interview inProgressInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.IN_PROGRESS).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(inProgressInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(Collections.emptyList());
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.empty());

        InterviewArchiveRecord archive = interviewService.getFullArchivedRecord(100L);

        assertNotNull(archive);
        assertNotNull(archive.getInterview());
        assertTrue(archive.getMessages().isEmpty());
        assertNull(archive.getReport());
    }

    @Test
    void getFullArchivedRecord_withNoReview_shouldReturnReportWithoutReviewDetails() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).ruleFamiliarity(7).communicationScore(8)
                .pressureScore(6).totalScore(7).aiComment("评语")
                .recommendationLabel("重点审查对话内容").build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(Collections.emptyList());
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));

        InterviewArchiveRecord archive = interviewService.getFullArchivedRecord(100L);

        assertNotNull(archive.getReport());
        assertNull(archive.getReport().getManualApproved());
        assertNull(archive.getReport().getReviewerComment());
        assertNull(archive.getReport().getReviewedAt());
    }

    @Test
    void getFullArchivedRecord_messagesPreservedInOrder() {
        Interview interview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.REVIEWED).build();
        LocalDateTime t1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2024, 1, 1, 10, 1);
        LocalDateTime t3 = LocalDateTime.of(2024, 1, 1, 10, 2);
        List<InterviewMessage> messages = List.of(
                InterviewMessage.builder().id(1L).interviewId(100L).role("AI").content("场景描述").timestamp(t1).build(),
                InterviewMessage.builder().id(2L).interviewId(100L).role("USER").content("我的回答").timestamp(t2).build(),
                InterviewMessage.builder().id(3L).interviewId(100L).role("AI").content("追问").timestamp(t3).build()
        );

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(interview));
        when(messageRepository.findByInterviewIdOrderByTimestamp(100L)).thenReturn(messages);
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.empty());

        InterviewArchiveRecord archive = interviewService.getFullArchivedRecord(100L);

        assertEquals(3, archive.getMessages().size());
        assertEquals("场景描述", archive.getMessages().get(0).getContent());
        assertEquals("我的回答", archive.getMessages().get(1).getContent());
        assertEquals("追问", archive.getMessages().get(2).getContent());
    }

    // --- Internship integration tests ---
    // Validates: Requirements 14.1, 6.12

    @Test
    void manualReview_approved_shouldCreateInternshipRecord() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(8).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();
        Internship internship = Internship.builder().id(50L).userId(10L).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipService.createForNewIntern(10L)).thenReturn(internship);

        interviewService.manualReview(100L, true, "表现优秀", "导师张三", null);

        verify(internshipService).createForNewIntern(10L);
    }

    @Test
    void manualReview_approved_withSuggestedMentorId_shouldAssignMentor() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(8).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();
        Internship internship = Internship.builder().id(50L).userId(10L).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipService.createForNewIntern(10L)).thenReturn(internship);

        interviewService.manualReview(100L, true, "表现优秀", "导师张三", 20L);

        verify(internshipService).createForNewIntern(10L);
        verify(internshipService).assignMentor(50L, 20L);
    }

    @Test
    void manualReview_approved_withoutSuggestedMentorId_shouldNotAssignMentor() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(8).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();
        Internship internship = Internship.builder().id(50L).userId(10L).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipService.createForNewIntern(10L)).thenReturn(internship);

        interviewService.manualReview(100L, true, "表现优秀", null, null);

        verify(internshipService).createForNewIntern(10L);
        verify(internshipService, never()).assignMentor(anyLong(), anyLong());
    }

    @Test
    void manualReview_approved_internshipCreationFails_shouldNotFailReview() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(8).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipService.createForNewIntern(10L)).thenThrow(new RuntimeException("数据库错误"));

        // Should not throw - internship creation failure is logged but doesn't fail the review
        InterviewReport result = interviewService.manualReview(100L, true, "表现优秀", null, null);

        assertNotNull(result);
        assertTrue(result.getManualApproved());
        assertEquals("通过", result.getReviewResult());
    }

    @Test
    void manualReview_approved_mentorAssignmentFails_shouldNotFailReview() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(8).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();
        Internship internship = Internship.builder().id(50L).userId(10L).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(internshipService.createForNewIntern(10L)).thenReturn(internship);
        doThrow(new BusinessException(400, "导师角色不符")).when(internshipService).assignMentor(50L, 99L);

        // Should not throw - mentor assignment failure is logged but doesn't fail the review
        InterviewReport result = interviewService.manualReview(100L, true, "表现优秀", null, 99L);

        assertNotNull(result);
        assertTrue(result.getManualApproved());
        verify(internshipService).createForNewIntern(10L);
        verify(internshipService).assignMentor(50L, 99L);
    }

    @Test
    void manualReview_rejected_shouldNotCreateInternship() {
        Interview completedInterview = Interview.builder()
                .id(100L).applicationId(1L).userId(10L)
                .status(InterviewStatus.COMPLETED).build();
        InterviewReport report = InterviewReport.builder()
                .id(1L).interviewId(100L).totalScore(3).build();
        User user = User.builder().id(10L).username("candidate").role(Role.APPLICANT).enabled(false).build();

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(completedInterview));
        when(reportRepository.findByInterviewId(100L)).thenReturn(Optional.of(report));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(InterviewReport.class))).thenAnswer(inv -> inv.getArgument(0));

        interviewService.manualReview(100L, false, "不合格", null, null);

        verify(internshipService, never()).createForNewIntern(anyLong());
        verify(internshipService, never()).assignMentor(anyLong(), anyLong());
    }
}
