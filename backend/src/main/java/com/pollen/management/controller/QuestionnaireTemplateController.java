package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.CreateTemplateRequest;
import com.pollen.management.dto.PublishVersionRequest;
import com.pollen.management.dto.UpdateTemplateRequest;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.service.QuestionnaireTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问卷模板管理控制器：CRUD + 发布 + 版本历史
 * 权限：仅 ADMIN、LEADER、VICE_LEADER 可访问（通过 SecurityConfig 配置）
 */
@RestController
@RequestMapping("/api/questionnaire/templates")
@RequiredArgsConstructor
public class QuestionnaireTemplateController {

    private final QuestionnaireTemplateService templateService;

    @PostMapping
    public ApiResponse<QuestionnaireTemplate> create(
            @Valid @RequestBody CreateTemplateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getDetails();
        QuestionnaireTemplate template = templateService.create(request, userId);
        return ApiResponse.success(template);
    }

    @PutMapping("/{id}")
    public ApiResponse<QuestionnaireTemplate> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        QuestionnaireTemplate template = templateService.update(id, request);
        return ApiResponse.success(template);
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionnaireTemplate> getById(@PathVariable Long id) {
        QuestionnaireTemplate template = templateService.getById(id);
        return ApiResponse.success(template);
    }

    @GetMapping
    public ApiResponse<List<QuestionnaireTemplate>> listAll() {
        List<QuestionnaireTemplate> templates = templateService.listAll();
        return ApiResponse.success(templates);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<QuestionnaireVersion> publish(
            @PathVariable Long id,
            @Valid @RequestBody PublishVersionRequest request) {
        QuestionnaireVersion version = templateService.publish(id, request.getVersionId());
        return ApiResponse.success(version);
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<QuestionnaireVersion>> getVersionHistory(@PathVariable Long id) {
        List<QuestionnaireVersion> versions = templateService.getVersionHistory(id);
        return ApiResponse.success(versions);
    }

    @GetMapping("/versions/{versionId}")
    public ApiResponse<QuestionnaireVersion> getVersion(@PathVariable Long versionId) {
        QuestionnaireVersion version = templateService.getVersion(versionId);
        return ApiResponse.success(version);
    }
}
