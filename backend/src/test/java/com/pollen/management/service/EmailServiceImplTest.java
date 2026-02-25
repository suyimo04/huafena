package com.pollen.management.service;

import com.pollen.management.dto.EmailLogQueryRequest;
import com.pollen.management.dto.SendEmailRequest;
import com.pollen.management.entity.EmailLog;
import com.pollen.management.entity.EmailTemplate;
import com.pollen.management.repository.EmailLogRepository;
import com.pollen.management.repository.EmailTemplateRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;
    @Mock
    private EmailLogRepository emailLogRepository;
    @Mock
    private EmailRateLimiter emailRateLimiter;
    @Mock
    private EmailSender emailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(
                emailTemplateRepository, emailLogRepository, emailRateLimiter, emailSender);
    }

    // --- renderTemplate tests ---

    @Test
    void renderTemplate_shouldReplaceAllPlaceholders() {
        String template = "Hello {{name}}, welcome to {{group}}!";
        Map<String, Object> vars = Map.of("name", "Alice", "group", "花粉小组");
        assertEquals("Hello Alice, welcome to 花粉小组!", emailService.renderTemplate(template, vars));
    }

    @Test
    void renderTemplate_shouldHandleNullVariables() {
        assertEquals("Hello {{name}}", emailService.renderTemplate("Hello {{name}}", null));
    }

    @Test
    void renderTemplate_shouldHandleNullTemplate() {
        assertNull(emailService.renderTemplate(null, Map.of("key", "val")));
    }

    @Test
    void renderTemplate_shouldReplaceNullValueWithEmptyString() {
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("key", null);
        assertEquals("Value: ", emailService.renderTemplate("Value: {{key}}", vars));
    }

    @Test
    void renderTemplate_shouldHandleMultipleOccurrences() {
        assertEquals("Bob said hello to Bob",
                emailService.renderTemplate("{{name}} said hello to {{name}}", Map.of("name", "Bob")));
    }

    @Test
    void renderTemplate_shouldLeaveUnmatchedPlaceholders() {
        assertEquals("Hello Alice, your role is {{role}}",
                emailService.renderTemplate("Hello {{name}}, your role is {{role}}", Map.of("name", "Alice")));
    }

    // --- sendEmail with rate limiter ---

    @Test
    void sendEmail_shouldMarkFailedWhenRateLimited() throws Exception {
        when(emailRateLimiter.isAllowed("test@example.com")).thenReturn(false);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        emailService.sendEmail(SendEmailRequest.builder()
                .recipient("test@example.com").subject("Test").body("<p>Hi</p>").html(true).build());

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(captor.capture());
        EmailLog finalLog = captor.getAllValues().get(1);
        assertEquals("FAILED", finalLog.getStatus());
        assertEquals("邮件发送频率超限", finalLog.getFailReason());
        verify(emailSender, never()).doSendEmail(any(), any(), any(), anyBoolean());
    }

    @Test
    void sendEmail_shouldSendAndRecordWhenAllowed() throws Exception {
        when(emailRateLimiter.isAllowed("test@example.com")).thenReturn(true);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailSender).doSendEmail(any(), any(), any(), anyBoolean());

        emailService.sendEmail(SendEmailRequest.builder()
                .recipient("test@example.com").subject("Test").body("<p>Hi</p>").html(true).build());

        verify(emailSender).doSendEmail("test@example.com", "Test", "<p>Hi</p>", true);
        verify(emailRateLimiter).recordSend("test@example.com");

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(captor.capture());
        EmailLog finalLog = captor.getAllValues().get(1);
        assertEquals("SENT", finalLog.getStatus());
        assertNotNull(finalLog.getSentAt());
    }

    @Test
    void sendEmail_shouldMarkFailedWhenSenderThrows() throws Exception {
        when(emailRateLimiter.isAllowed("test@example.com")).thenReturn(true);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(emailSender)
                .doSendEmail(any(), any(), any(), anyBoolean());

        emailService.sendEmail(SendEmailRequest.builder()
                .recipient("test@example.com").subject("Test").body("<p>Hi</p>").html(true).build());

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(captor.capture());
        EmailLog finalLog = captor.getAllValues().get(1);
        assertEquals("FAILED", finalLog.getStatus());
        assertEquals(3, finalLog.getRetryCount());
        assertNotNull(finalLog.getFailReason());
    }

    // --- sendTemplateEmail with rate limiter ---

    @Test
    void sendTemplateEmail_shouldThrowWhenTemplateNotFound() {
        when(emailTemplateRepository.findByTemplateCode("NONEXISTENT")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () ->
                emailService.sendTemplateEmail("NONEXISTENT", Map.of(), "test@example.com"));
    }

    @Test
    void sendTemplateEmail_shouldMarkFailedWhenRateLimited() {
        EmailTemplate template = EmailTemplate.builder()
                .templateCode("INTERVIEW_NOTIFICATION")
                .subjectTemplate("面试通知 - {{name}}")
                .bodyTemplate("<p>Hello {{name}}</p>")
                .build();
        when(emailTemplateRepository.findByTemplateCode("INTERVIEW_NOTIFICATION"))
                .thenReturn(Optional.of(template));
        when(emailRateLimiter.isAllowed("test@example.com")).thenReturn(false);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        emailService.sendTemplateEmail("INTERVIEW_NOTIFICATION", Map.of("name", "张三"), "test@example.com");

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(captor.capture());
        EmailLog finalLog = captor.getAllValues().get(1);
        assertEquals("FAILED", finalLog.getStatus());
        assertEquals("邮件发送频率超限", finalLog.getFailReason());
    }

    @Test
    void sendTemplateEmail_shouldSendSuccessfully() throws Exception {
        EmailTemplate template = EmailTemplate.builder()
                .templateCode("INTERVIEW_NOTIFICATION")
                .subjectTemplate("面试通知 - {{name}}")
                .bodyTemplate("<p>Hello {{name}}</p>")
                .build();
        when(emailTemplateRepository.findByTemplateCode("INTERVIEW_NOTIFICATION"))
                .thenReturn(Optional.of(template));
        when(emailRateLimiter.isAllowed("test@example.com")).thenReturn(true);
        when(emailLogRepository.save(any(EmailLog.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailSender).doSendEmail(any(), any(), any(), anyBoolean());

        emailService.sendTemplateEmail("INTERVIEW_NOTIFICATION", Map.of("name", "张三"), "test@example.com");

        verify(emailSender).doSendEmail("test@example.com", "面试通知 - 张三", "<p>Hello 张三</p>", true);
        verify(emailRateLimiter).recordSend("test@example.com");

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, atLeast(1)).save(captor.capture());
        EmailLog firstSave = captor.getAllValues().get(0);
        assertEquals("面试通知 - 张三", firstSave.getSubject());
        assertEquals("INTERVIEW_NOTIFICATION", firstSave.getTemplateCode());
    }

    // --- getTemplates / getLogs ---

    @Test
    void getTemplates_shouldReturnAllTemplates() {
        List<EmailTemplate> templates = List.of(
                EmailTemplate.builder().templateCode("A").subjectTemplate("S").bodyTemplate("B").build(),
                EmailTemplate.builder().templateCode("B").subjectTemplate("S2").bodyTemplate("B2").build());
        when(emailTemplateRepository.findAll()).thenReturn(templates);
        assertEquals(2, emailService.getTemplates().size());
    }

    @Test
    void getLogs_shouldReturnPagedResults() {
        EmailLog log1 = EmailLog.builder().recipient("a@b.com").subject("S1").status("SENT").build();
        Page<EmailLog> page = new PageImpl<>(List.of(log1), PageRequest.of(0, 20), 1);
        when(emailLogRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class))).thenReturn(page);

        Page<EmailLog> result = emailService.getLogs(EmailLogQueryRequest.builder().page(0).size(20).build());
        assertEquals(1, result.getTotalElements());
        assertEquals("a@b.com", result.getContent().get(0).getRecipient());
    }
}
