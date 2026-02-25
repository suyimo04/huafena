package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.BatchOperationRequest;
import com.pollen.management.dto.CreateApplicationRequest;
import com.pollen.management.dto.GeneratePublicLinkRequest;
import com.pollen.management.dto.InitialReviewRequest;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.ApplicationTimeline;
import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.service.ApplicationService;
import com.pollen.management.service.PublicLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 申请流程控制器：申请创建、列表查询、初审、公开链接管理
 * 权限：ADMIN、LEADER、VICE_LEADER（通过 SecurityConfig 配置）
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final PublicLinkService publicLinkService;

    @PostMapping("/registration")
    public ApiResponse<Application> createFromRegistration(
            @Valid @RequestBody CreateApplicationRequest request) {
        Application application = applicationService.createFromRegistration(
                request.getUserId(), request.getQuestionnaireResponseId(), request.getFormData());
        return ApiResponse.success(application);
    }

    @GetMapping
    public ApiResponse<List<Application>> listAll() {
        List<Application> applications = applicationService.listAll();
        return ApiResponse.success(applications);
    }

    @PostMapping("/{id}/initial-review")
    public ApiResponse<Void> initialReview(
            @PathVariable Long id,
            @Valid @RequestBody InitialReviewRequest request) {
        applicationService.initialReview(id, request.getApproved());
        return ApiResponse.success(null);
    }

    @PostMapping("/public-links/generate")
    public ApiResponse<PublicLink> generatePublicLink(
            @Valid @RequestBody GeneratePublicLinkRequest request,
            Authentication authentication) {
        Long createdBy = (Long) authentication.getDetails();
        PublicLink link = publicLinkService.generate(request.getTemplateId(), createdBy);
        return ApiResponse.success(link);
    }

    @GetMapping("/public-links")
    public ApiResponse<List<PublicLink>> listPublicLinks() {
        List<PublicLink> links = publicLinkService.listAll();
        return ApiResponse.success(links);
    }

    @PostMapping("/batch-approve")
    public ApiResponse<Void> batchApprove(@Valid @RequestBody BatchOperationRequest request) {
        applicationService.batchApprove(request.getApplicationIds());
        return ApiResponse.success(null);
    }

    @PostMapping("/batch-reject")
    public ApiResponse<Void> batchReject(@Valid @RequestBody BatchOperationRequest request) {
        applicationService.batchReject(request.getApplicationIds());
        return ApiResponse.success(null);
    }

    @PostMapping("/batch-notify-interview")
    public ApiResponse<Void> batchNotifyInterview(@Valid @RequestBody BatchOperationRequest request) {
        applicationService.batchNotifyInterview(request.getApplicationIds());
        return ApiResponse.success(null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) ApplicationStatus status) {
        byte[] excelData = applicationService.exportToExcel(status);
        String filename = "applications_export.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<List<ApplicationTimeline>> getTimeline(@PathVariable Long id) {
        List<ApplicationTimeline> timeline = applicationService.getTimeline(id);
        return ApiResponse.success(timeline);
    }
}
