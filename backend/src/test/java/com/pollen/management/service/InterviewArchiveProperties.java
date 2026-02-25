package com.pollen.management.service;

import com.pollen.management.dto.InterviewArchiveRecord;
import com.pollen.management.entity.Interview;
import com.pollen.management.entity.InterviewMessage;
import com.pollen.management.entity.InterviewReport;
import com.pollen.management.entity.enums.InterviewStatus;
import com.pollen.management.repository.*;
import com.pollen.management.util.BusinessException;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Feature: pollen-group-management, Property 18: 面试记录持久化
 * **Validates: Requirements 5.14**
 *
 * Property 18: For any completed interview, getFullArchivedRecord should return
 * the complete record with all messages in timestamp order and the report if available.
 * All fields (dialogue content, scores, review opinions) are preserved exactly as stored.
 * Interview not found throws 404.
 */
class InterviewArchiveProperties {

    private InterviewServiceImpl createService(
            InterviewRepository interviewRepo,
            InterviewMessageRepository messageRepo,
            InterviewReportRepository reportRepo) {
        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        InterviewScenarioService scenarioService = Mockito.mock(InterviewScenarioService.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        InternshipService internshipService = Mockito.mock(InternshipService.class);
        RoleChangeHistoryRepository roleChangeHistoryRepo = Mockito.mock(RoleChangeHistoryRepository.class);
        return new InterviewServiceImpl(interviewRepo, messageRepo, reportRepo, appRepo, scenarioService, userRepo, internshipService, roleChangeHistoryRepo);
    }

    // ========== Property 18a: Full record with messages and report is returned intact ==========

    /**
     * For any interview with messages and a report, getFullArchivedRecord returns all data intact.
     */
    @Property(tries = 100)
    void fullArchivedRecordReturnsAllDataIntact(
            @ForAll("interviewIds") Long interviewId,
            @ForAll("messageCounts") int messageCount,
            @ForAll("scores") int ruleFamiliarity,
            @ForAll("scores") int communicationScore,
            @ForAll("scores") int pressureScore) {

        InterviewRepository interviewRepo = Mockito.mock(InterviewRepository.class);
        InterviewMessageRepository messageRepo = Mockito.mock(InterviewMessageRepository.class);
        InterviewReportRepository reportRepo = Mockito.mock(InterviewReportRepository.class);
        InterviewServiceImpl service = createService(interviewRepo, messageRepo, reportRepo);

        Interview interview = Interview.builder()
                .id(interviewId)
                .applicationId(interviewId + 1000)
                .userId(interviewId + 2000)
                .status(InterviewStatus.COMPLETED)
                .scenarioId("conflict_handling")
                .difficultyLevel("standard")
                .build();

        LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        List<InterviewMessage> messages = IntStream.range(0, messageCount)
                .mapToObj(i -> InterviewMessage.builder()
                        .id((long) (i + 1))
                        .interviewId(interviewId)
                        .role(i % 2 == 0 ? "AI" : "USER")
                        .content("Message content #" + i)
                        .timestamp(baseTime.plusMinutes(i))
                        .timeLimitSeconds(60)
                        .build())
                .collect(Collectors.toList());

        int totalScore = (ruleFamiliarity + communicationScore + pressureScore) / 3;
        InterviewReport report = InterviewReport.builder()
                .id(interviewId * 10)
                .interviewId(interviewId)
                .ruleFamiliarity(ruleFamiliarity)
                .communicationScore(communicationScore)
                .pressureScore(pressureScore)
                .totalScore(totalScore)
                .aiComment("AI评语 for interview " + interviewId)
                .reviewerComment("复审意见 for interview " + interviewId)
                .reviewResult("APPROVED")
                .suggestedMentor("mentor_" + interviewId)
                .recommendationLabel("建议通过")
                .manualApproved(true)
                .build();

        when(interviewRepo.findById(interviewId)).thenReturn(Optional.of(interview));
        when(messageRepo.findByInterviewIdOrderByTimestamp(interviewId)).thenReturn(messages);
        when(reportRepo.findByInterviewId(interviewId)).thenReturn(Optional.of(report));

        InterviewArchiveRecord record = service.getFullArchivedRecord(interviewId);

        assertThat(record.getInterview()).isSameAs(interview);
        assertThat(record.getMessages()).hasSize(messageCount);
        assertThat(record.getReport()).isSameAs(report);
    }

    // ========== Property 18b: Messages are returned in timestamp order ==========

    /**
     * Messages in the archived record are always ordered by timestamp.
     */
    @Property(tries = 100)
    void archivedMessagesAreInTimestampOrder(
            @ForAll("interviewIds") Long interviewId,
            @ForAll("messageCounts") int messageCount) {

        InterviewRepository interviewRepo = Mockito.mock(InterviewRepository.class);
        InterviewMessageRepository messageRepo = Mockito.mock(InterviewMessageRepository.class);
        InterviewReportRepository reportRepo = Mockito.mock(InterviewReportRepository.class);
        InterviewServiceImpl service = createService(interviewRepo, messageRepo, reportRepo);

        Interview interview = Interview.builder()
                .id(interviewId)
                .applicationId(1L)
                .userId(1L)
                .status(InterviewStatus.COMPLETED)
                .build();

        LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        // Create messages already sorted by timestamp (simulating repository behavior)
        List<InterviewMessage> sortedMessages = IntStream.range(0, messageCount)
                .mapToObj(i -> InterviewMessage.builder()
                        .id((long) (i + 1))
                        .interviewId(interviewId)
                        .role(i % 2 == 0 ? "AI" : "USER")
                        .content("Msg " + i)
                        .timestamp(baseTime.plusMinutes(i))
                        .build())
                .collect(Collectors.toList());

        when(interviewRepo.findById(interviewId)).thenReturn(Optional.of(interview));
        when(messageRepo.findByInterviewIdOrderByTimestamp(interviewId)).thenReturn(sortedMessages);
        when(reportRepo.findByInterviewId(interviewId)).thenReturn(Optional.empty());

        InterviewArchiveRecord record = service.getFullArchivedRecord(interviewId);

        List<InterviewMessage> resultMessages = record.getMessages();
        for (int i = 1; i < resultMessages.size(); i++) {
            assertThat(resultMessages.get(i).getTimestamp())
                    .as("Message at index %d should be after message at index %d", i, i - 1)
                    .isAfterOrEqualTo(resultMessages.get(i - 1).getTimestamp());
        }
    }

    // ========== Property 18c: All fields are preserved exactly as stored ==========

    /**
     * All report fields (scores, review opinions, comments) are preserved exactly.
     */
    @Property(tries = 100)
    void allReportFieldsArePreservedExactly(
            @ForAll("interviewIds") Long interviewId,
            @ForAll("scores") int ruleFamiliarity,
            @ForAll("scores") int communicationScore,
            @ForAll("scores") int pressureScore,
            @ForAll("reviewResults") String reviewResult,
            @ForAll("booleans") boolean manualApproved) {

        InterviewRepository interviewRepo = Mockito.mock(InterviewRepository.class);
        InterviewMessageRepository messageRepo = Mockito.mock(InterviewMessageRepository.class);
        InterviewReportRepository reportRepo = Mockito.mock(InterviewReportRepository.class);
        InterviewServiceImpl service = createService(interviewRepo, messageRepo, reportRepo);

        Interview interview = Interview.builder()
                .id(interviewId).applicationId(1L).userId(1L)
                .status(InterviewStatus.REVIEWED).build();

        int totalScore = (ruleFamiliarity + communicationScore + pressureScore) / 3;
        String aiComment = "AI评语_" + interviewId;
        String reviewerComment = "复审评语_" + interviewId;
        String suggestedMentor = "导师_" + interviewId;
        String label = ruleFamiliarity >= 8 ? "建议通过" : "建议拒绝";

        InterviewReport report = InterviewReport.builder()
                .id(interviewId * 10)
                .interviewId(interviewId)
                .ruleFamiliarity(ruleFamiliarity)
                .communicationScore(communicationScore)
                .pressureScore(pressureScore)
                .totalScore(totalScore)
                .aiComment(aiComment)
                .reviewerComment(reviewerComment)
                .reviewResult(reviewResult)
                .suggestedMentor(suggestedMentor)
                .recommendationLabel(label)
                .manualApproved(manualApproved)
                .build();

        when(interviewRepo.findById(interviewId)).thenReturn(Optional.of(interview));
        when(messageRepo.findByInterviewIdOrderByTimestamp(interviewId)).thenReturn(List.of());
        when(reportRepo.findByInterviewId(interviewId)).thenReturn(Optional.of(report));

        InterviewArchiveRecord record = service.getFullArchivedRecord(interviewId);
        InterviewReport resultReport = record.getReport();

        assertThat(resultReport.getRuleFamiliarity()).isEqualTo(ruleFamiliarity);
        assertThat(resultReport.getCommunicationScore()).isEqualTo(communicationScore);
        assertThat(resultReport.getPressureScore()).isEqualTo(pressureScore);
        assertThat(resultReport.getTotalScore()).isEqualTo(totalScore);
        assertThat(resultReport.getAiComment()).isEqualTo(aiComment);
        assertThat(resultReport.getReviewerComment()).isEqualTo(reviewerComment);
        assertThat(resultReport.getReviewResult()).isEqualTo(reviewResult);
        assertThat(resultReport.getSuggestedMentor()).isEqualTo(suggestedMentor);
        assertThat(resultReport.getRecommendationLabel()).isEqualTo(label);
        assertThat(resultReport.getManualApproved()).isEqualTo(manualApproved);
    }

    // ========== Property 18d: Interview not found throws 404 ==========

    /**
     * When interview does not exist, getFullArchivedRecord throws BusinessException with code 404.
     */
    @Property(tries = 100)
    void interviewNotFoundThrows404(
            @ForAll("interviewIds") Long interviewId) {

        InterviewRepository interviewRepo = Mockito.mock(InterviewRepository.class);
        InterviewMessageRepository messageRepo = Mockito.mock(InterviewMessageRepository.class);
        InterviewReportRepository reportRepo = Mockito.mock(InterviewReportRepository.class);
        InterviewServiceImpl service = createService(interviewRepo, messageRepo, reportRepo);

        when(interviewRepo.findById(interviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFullArchivedRecord(interviewId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode())
                            .as("Interview not found must return code 404 for id=%d", interviewId)
                            .isEqualTo(404);
                });
    }

    // ========== Property 18e: Record without report returns null report ==========

    /**
     * For an interview without a report, the archived record has a null report field.
     */
    @Property(tries = 100)
    void archivedRecordWithoutReportHasNullReport(
            @ForAll("interviewIds") Long interviewId,
            @ForAll("messageCounts") int messageCount) {

        InterviewRepository interviewRepo = Mockito.mock(InterviewRepository.class);
        InterviewMessageRepository messageRepo = Mockito.mock(InterviewMessageRepository.class);
        InterviewReportRepository reportRepo = Mockito.mock(InterviewReportRepository.class);
        InterviewServiceImpl service = createService(interviewRepo, messageRepo, reportRepo);

        Interview interview = Interview.builder()
                .id(interviewId).applicationId(1L).userId(1L)
                .status(InterviewStatus.IN_PROGRESS).build();

        LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        List<InterviewMessage> messages = IntStream.range(0, messageCount)
                .mapToObj(i -> InterviewMessage.builder()
                        .id((long) (i + 1))
                        .interviewId(interviewId)
                        .role("AI")
                        .content("Content " + i)
                        .timestamp(baseTime.plusMinutes(i))
                        .build())
                .collect(Collectors.toList());

        when(interviewRepo.findById(interviewId)).thenReturn(Optional.of(interview));
        when(messageRepo.findByInterviewIdOrderByTimestamp(interviewId)).thenReturn(messages);
        when(reportRepo.findByInterviewId(interviewId)).thenReturn(Optional.empty());

        InterviewArchiveRecord record = service.getFullArchivedRecord(interviewId);

        assertThat(record.getInterview()).isNotNull();
        assertThat(record.getMessages()).hasSize(messageCount);
        assertThat(record.getReport()).isNull();
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Long> interviewIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Integer> messageCounts() {
        return Arbitraries.integers().between(1, 20);
    }

    @Provide
    Arbitrary<Integer> scores() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<String> reviewResults() {
        return Arbitraries.of("APPROVED", "REJECTED");
    }

    @Provide
    Arbitrary<Boolean> booleans() {
        return Arbitraries.of(true, false);
    }
}
