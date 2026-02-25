package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.SubmitResponseRequest;
import com.pollen.management.entity.QuestionnaireResponse;
import com.pollen.management.service.QuestionnaireResponseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 问卷回答控制器：提交和查询端点
 * 需要认证用户访问
 */
@RestController
@RequestMapping("/api/questionnaire/responses")
@RequiredArgsConstructor
public class QuestionnaireResponseController {

    private final QuestionnaireResponseService responseService;

    @PostMapping
    public ApiResponse<QuestionnaireResponse> submit(
            @Valid @RequestBody SubmitResponseRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getDetails();
        QuestionnaireResponse response = responseService.submit(
                request.getVersionId(), userId, request.getAnswers());
        return ApiResponse.success(response);
    }

    @GetMapping("/application/{applicationId}")
    public ApiResponse<QuestionnaireResponse> getByApplicationId(
            @PathVariable Long applicationId) {
        QuestionnaireResponse response = responseService.getByApplicationId(applicationId);
        return ApiResponse.success(response);
    }
}
