package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.EmailConfig;
import com.pollen.management.entity.EmailLog;
import com.pollen.management.entity.EmailTemplate;
import com.pollen.management.service.EmailConfigService;
import com.pollen.management.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邮件服务控制器：发送邮件、模板列表、发送日志、SMTP 配置。
 * 权限：ADMIN/LEADER 可发送邮件和管理配置。
 */
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final EmailConfigService emailConfigService;

    /**
     * 发送邮件（异步）
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        emailService.sendEmail(request);
        return ApiResponse.success(null);
    }

    /**
     * 获取邮件模板列表
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<EmailTemplate>> getTemplates() {
        return ApiResponse.success(emailService.getTemplates());
    }

    /**
     * 获取发送日志（分页）
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Page<EmailLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        EmailLogQueryRequest request = EmailLogQueryRequest.builder()
                .page(page)
                .size(size)
                .build();
        return ApiResponse.success(emailService.getLogs(request));
    }

    /**
     * 获取 SMTP 配置（密码脱敏）
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<EmailConfig> getConfig() {
        return ApiResponse.success(emailConfigService.getConfig());
    }

    /**
     * 更新 SMTP 配置
     */
    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> updateConfig(@Valid @RequestBody UpdateEmailConfigRequest request) {
        emailConfigService.updateConfig(request);
        return ApiResponse.success(null);
    }
}
