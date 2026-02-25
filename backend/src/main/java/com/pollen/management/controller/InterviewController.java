package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.Interview;
import com.pollen.management.entity.InterviewMessage;
import com.pollen.management.entity.InterviewReport;
import com.pollen.management.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 面试控制器：启动面试、对话交互、结束面试、查看报告、人工复审
 * 基础权限：ADMIN、LEADER、VICE_LEADER（通过 SecurityConfig 配置）
 * 人工复审：仅 ADMIN、LEADER（通过 @PreAuthorize 限制）
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ApiResponse<Interview> startInterview(
            @Valid @RequestBody StartInterviewRequest request) {
        Interview interview = interviewService.startInterview(
                request.getApplicationId(), request.getScenarioId());
        return ApiResponse.success(interview);
    }

    @PostMapping("/{id}/message")
    public ApiResponse<InterviewMessage> processMessage(
            @PathVariable Long id,
            @Valid @RequestBody InterviewMessageRequest request) {
        InterviewMessage message = interviewService.processMessage(id, request.getMessage());
        return ApiResponse.success(message);
    }

    @PostMapping("/{id}/end")
    public ApiResponse<InterviewReport> endInterview(@PathVariable Long id) {
        InterviewReport report = interviewService.endInterview(id);
        return ApiResponse.success(report);
    }

    @GetMapping("/{id}")
    public ApiResponse<Interview> getInterview(@PathVariable Long id) {
        Interview interview = interviewService.getInterview(id);
        return ApiResponse.success(interview);
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<InterviewMessage>> getMessages(@PathVariable Long id) {
        List<InterviewMessage> messages = interviewService.getMessages(id);
        return ApiResponse.success(messages);
    }

    @GetMapping("/{id}/report")
    public ApiResponse<InterviewReport> getReport(@PathVariable Long id) {
        InterviewReport report = interviewService.getReport(id);
        return ApiResponse.success(report);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<InterviewReport> manualReview(
            @PathVariable Long id,
            @Valid @RequestBody ManualReviewRequest request) {
        InterviewReport report = interviewService.manualReview(
                id, request.getApproved(), request.getReviewComment(), request.getSuggestedMentor(), request.getSuggestedMentorId());
        return ApiResponse.success(report);
    }

    @GetMapping("/{id}/archive")
    public ApiResponse<InterviewArchiveRecord> getArchive(@PathVariable Long id) {
        InterviewArchiveRecord record = interviewService.getFullArchivedRecord(id);
        return ApiResponse.success(record);
    }
}
