package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.PublicSubmitRequest;
import com.pollen.management.entity.QuestionnaireVersion;
import com.pollen.management.service.PublicLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公开问卷控制器：无需认证即可访问
 * 通过公开链接 token 获取问卷和提交回答
 */
@RestController
@RequestMapping("/api/public/questionnaire")
@RequiredArgsConstructor
public class PublicQuestionnaireController {

    private final PublicLinkService publicLinkService;

    @GetMapping("/{linkToken}")
    public ApiResponse<QuestionnaireVersion> getByToken(@PathVariable String linkToken) {
        QuestionnaireVersion version = publicLinkService.getQuestionnaireByToken(linkToken);
        return ApiResponse.success(version);
    }

    @PostMapping("/{linkToken}/submit")
    public ApiResponse<Map<String, Object>> submitByToken(
            @PathVariable String linkToken,
            @Valid @RequestBody PublicSubmitRequest request) {
        Map<String, Object> result = publicLinkService.submitByToken(linkToken, request.getAnswers());
        return ApiResponse.success(result);
    }
}
