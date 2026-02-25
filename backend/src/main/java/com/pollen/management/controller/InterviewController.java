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
 * 访问控制（Requirements 17.6）：
 * - ADMIN/LEADER：完整读写访问
 * - VICE_LEADER：只读访问（仅 GET 端点）
 * - 其他角色：403（通过 SecurityConfig URL 级别拦截）
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Interview> startInterview(
            @Valid @RequestBody StartInterviewRequest request) {
        Interview interview = interviewService.startInterview(
                request.getApplicationId(), request.getScenarioId());
        return ApiResponse.success(interview);
    }

    @PostMapping("/{id}/message")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<InterviewMessage> processMessage(
            @PathVariable Long id,
            @Valid @RequestBody InterviewMessageRequest request) {
        InterviewMessage message = interviewService.processMessage(id, request.getMessage());
        return ApiResponse.success(message);
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<InterviewReport> endInterview(@PathVariable Long id) {
        InterviewReport report = interviewService.endInterview(id);
        return ApiResponse.success(report);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<Interview> getInterview(@PathVariable Long id) {
        Interview interview = interviewService.getInterview(id);
        return ApiResponse.success(interview);
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<List<InterviewMessage>> getMessages(@PathVariable Long id) {
        List<InterviewMessage> messages = interviewService.getMessages(id);
        return ApiResponse.success(messages);
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<InterviewArchiveRecord> getArchive(@PathVariable Long id) {
        InterviewArchiveRecord record = interviewService.getFullArchivedRecord(id);
        return ApiResponse.success(record);
    }
}

