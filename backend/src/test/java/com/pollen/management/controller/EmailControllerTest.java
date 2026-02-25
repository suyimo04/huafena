package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.EmailConfig;
import com.pollen.management.entity.EmailLog;
import com.pollen.management.entity.EmailTemplate;
import com.pollen.management.service.EmailConfigService;
import com.pollen.management.service.EmailService;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailConfigService emailConfigService;

    @InjectMocks
    private EmailController controller;

    // --- POST /api/emails/send ---

    @Test
    void sendEmail_shouldDelegateToServiceAndReturnSuccess() {
        var request = SendEmailRequest.builder()
                .recipient("test@example.com")
                .subject("测试邮件")
                .body("<p>Hello</p>")
                .html(true)
                .build();

        ApiResponse<Void> response = controller.sendEmail(request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(emailService).sendEmail(request);
    }

    @Test
    void sendEmail_shouldPassRequestToService() {
        var request = SendEmailRequest.builder()
                .recipient("user@qq.com")
                .subject("面试通知")
                .body("请参加面试")
                .html(false)
                .build();

        controller.sendEmail(request);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(emailService).sendEmail(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo("user@qq.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("面试通知");
    }

    // --- GET /api/emails/templates ---

    @Test
    void getTemplates_shouldReturnTemplateList() {
        var template1 = EmailTemplate.builder()
                .id(1L)
                .templateCode("INTERVIEW_NOTIFICATION")
                .subjectTemplate("面试通知 - {{name}}")
                .bodyTemplate("<p>Dear {{name}}</p>")
                .build();
        var template2 = EmailTemplate.builder()
                .id(2L)
                .templateCode("REVIEW_RESULT_APPROVED")
                .subjectTemplate("审核通过")
                .bodyTemplate("<p>恭喜</p>")
                .build();
        when(emailService.getTemplates()).thenReturn(List.of(template1, template2));

        ApiResponse<List<EmailTemplate>> response = controller.getTemplates();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getTemplateCode()).isEqualTo("INTERVIEW_NOTIFICATION");
    }

    @Test
    void getTemplates_emptyList_shouldReturnEmptySuccess() {
        when(emailService.getTemplates()).thenReturn(List.of());

        ApiResponse<List<EmailTemplate>> response = controller.getTemplates();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEmpty();
    }

    // --- GET /api/emails/logs ---

    @Test
    void getLogs_shouldReturnPaginatedLogs() {
        var log1 = EmailLog.builder()
                .id(1L)
                .recipient("a@test.com")
                .subject("Test")
                .status("SENT")
                .sentAt(LocalDateTime.now())
                .build();
        Page<EmailLog> page = new PageImpl<>(List.of(log1));
        when(emailService.getLogs(any(EmailLogQueryRequest.class))).thenReturn(page);

        ApiResponse<Page<EmailLog>> response = controller.getLogs(0, 20);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getContent()).hasSize(1);
        assertThat(response.getData().getContent().get(0).getRecipient()).isEqualTo("a@test.com");
    }

    @Test
    void getLogs_shouldPassPaginationParams() {
        Page<EmailLog> page = new PageImpl<>(List.of());
        when(emailService.getLogs(any(EmailLogQueryRequest.class))).thenReturn(page);

        controller.getLogs(2, 10);

        ArgumentCaptor<EmailLogQueryRequest> captor = ArgumentCaptor.forClass(EmailLogQueryRequest.class);
        verify(emailService).getLogs(captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(10);
    }

    // --- GET /api/emails/config ---

    @Test
    void getConfig_shouldReturnMaskedConfig() {
        var config = EmailConfig.builder()
                .id(1L)
                .smtpHost("smtp.qq.com")
                .smtpPort(465)
                .smtpUsername("admin@qq.com")
                .smtpPasswordEncrypted("****")
                .senderName("花粉管理系统")
                .sslEnabled(true)
                .build();
        when(emailConfigService.getConfig()).thenReturn(config);

        ApiResponse<EmailConfig> response = controller.getConfig();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getSmtpHost()).isEqualTo("smtp.qq.com");
        assertThat(response.getData().getSmtpPasswordEncrypted()).isEqualTo("****");
    }

    @Test
    void getConfig_notFound_shouldPropagateException() {
        when(emailConfigService.getConfig())
                .thenThrow(new BusinessException(404, "SMTP 配置不存在"));

        assertThatThrownBy(() -> controller.getConfig())
                .isInstanceOf(BusinessException.class)
                .hasMessage("SMTP 配置不存在");
    }

    // --- PUT /api/emails/config ---

    @Test
    void updateConfig_shouldDelegateToService() {
        var request = UpdateEmailConfigRequest.builder()
                .smtpHost("smtp.163.com")
                .smtpPort(465)
                .smtpUsername("user@163.com")
                .smtpPassword("newpassword")
                .senderName("花粉系统")
                .sslEnabled(true)
                .build();

        ApiResponse<Void> response = controller.updateConfig(request);

        assertThat(response.getCode()).isEqualTo(200);
        verify(emailConfigService).updateConfig(request);
    }

    @Test
    void updateConfig_withMaskedPassword_shouldStillDelegate() {
        var request = UpdateEmailConfigRequest.builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .smtpUsername("user@gmail.com")
                .smtpPassword("****")
                .senderName("Pollen System")
                .sslEnabled(true)
                .build();

        controller.updateConfig(request);

        verify(emailConfigService).updateConfig(request);
    }
}
