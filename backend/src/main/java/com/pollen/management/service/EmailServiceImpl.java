package com.pollen.management.service;

import com.pollen.management.dto.EmailLogQueryRequest;
import com.pollen.management.dto.SendEmailRequest;
import com.pollen.management.entity.EmailLog;
import com.pollen.management.entity.EmailTemplate;
import com.pollen.management.repository.EmailLogRepository;
import com.pollen.management.repository.EmailTemplateRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailRateLimiter emailRateLimiter;
    private final EmailSender emailSender;

    @Override
    @Async
    public void sendEmail(SendEmailRequest request) {
        EmailLog emailLog = EmailLog.builder()
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .status("PENDING")
                .retryCount(0)
                .build();
        emailLogRepository.save(emailLog);

        // 防滥发检查
        if (!emailRateLimiter.isAllowed(request.getRecipient())) {
            log.warn("邮件发送频率超限: recipient={}", request.getRecipient());
            emailLog.setStatus("FAILED");
            emailLog.setFailReason("邮件发送频率超限");
            emailLogRepository.save(emailLog);
            return;
        }

        try {
            emailSender.doSendEmail(
                    request.getRecipient(),
                    request.getSubject(),
                    request.getBody(),
                    request.isHtml()
            );
            emailRateLimiter.recordSend(request.getRecipient());
            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("邮件发送失败: recipient={}, subject={}", request.getRecipient(), request.getSubject(), e);
            emailLog.setStatus("FAILED");
            emailLog.setFailReason(e.getMessage());
            emailLog.setRetryCount(3); // 重试 3 次后最终失败
        }
        emailLogRepository.save(emailLog);
    }

    @Override
    @Async
    public void sendTemplateEmail(String templateCode, Map<String, Object> variables, String recipient) {
        EmailTemplate template = emailTemplateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new BusinessException("邮件模板不存在: " + templateCode));

        String subject = renderTemplate(template.getSubjectTemplate(), variables);
        String body = renderTemplate(template.getBodyTemplate(), variables);

        EmailLog emailLog = EmailLog.builder()
                .recipient(recipient)
                .subject(subject)
                .templateCode(templateCode)
                .status("PENDING")
                .retryCount(0)
                .build();
        emailLogRepository.save(emailLog);

        // 防滥发检查
        if (!emailRateLimiter.isAllowed(recipient)) {
            log.warn("模板邮件发送频率超限: templateCode={}, recipient={}", templateCode, recipient);
            emailLog.setStatus("FAILED");
            emailLog.setFailReason("邮件发送频率超限");
            emailLogRepository.save(emailLog);
            return;
        }

        try {
            emailSender.doSendEmail(recipient, subject, body, true);
            emailRateLimiter.recordSend(recipient);
            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("模板邮件发送失败: templateCode={}, recipient={}", templateCode, recipient, e);
            emailLog.setStatus("FAILED");
            emailLog.setFailReason(e.getMessage());
            emailLog.setRetryCount(3);
        }
        emailLogRepository.save(emailLog);
    }

    @Override
    public List<EmailTemplate> getTemplates() {
        return emailTemplateRepository.findAll();
    }

    @Override
    public Page<EmailLog> getLogs(EmailLogQueryRequest request) {
        return emailLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(request.getPage(), request.getSize()));
    }

    /**
     * 渲染模板：将 {{key}} 占位符替换为 variables 中对应的值。
     */
    String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
