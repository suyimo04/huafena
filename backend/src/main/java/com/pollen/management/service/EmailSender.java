package com.pollen.management.service;

import com.pollen.management.entity.EmailConfig;
import com.pollen.management.repository.EmailConfigRepository;
import com.pollen.management.util.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 邮件发送器：负责实际 SMTP 发送，支持 @Retryable 重试（最多 3 次，间隔 30 秒）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final EmailConfigRepository emailConfigRepository;

    /**
     * 执行实际的 SMTP 邮件发送，支持自动重试。
     * 最多重试 3 次，每次间隔 30 秒。
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 30000)
    )
    public void doSendEmail(String to, String subject, String body, boolean html) throws Exception {
        log.info("尝试发送邮件: to={}, subject={}", to, subject);
        JavaMailSender mailSender = createMailSender();
        sendMimeMessage(mailSender, to, subject, body, html);
        log.info("邮件发送成功: to={}, subject={}", to, subject);
    }

    @Recover
    public void recoverSendEmail(Exception e, String to, String subject, String body, boolean html) {
        log.error("邮件发送最终失败（已重试 3 次）: to={}, subject={}, error={}", to, subject, e.getMessage());
        throw new RuntimeException("邮件发送失败（已重试 3 次）: " + e.getMessage(), e);
    }

    JavaMailSender createMailSender() {
        EmailConfig config = emailConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("SMTP 邮件配置不存在，请先配置邮件服务"));

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());
        mailSender.setUsername(config.getSmtpUsername());
        mailSender.setPassword(config.getSmtpPasswordEncrypted());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        if (Boolean.TRUE.equals(config.getSslEnabled())) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", config.getSmtpHost());
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return mailSender;
    }

    private void sendMimeMessage(JavaMailSender mailSender, String to, String subject, String body, boolean html)
            throws MessagingException {
        EmailConfig config = emailConfigRepository.findAll().stream().findFirst().orElse(null);
        String from = (config != null && config.getSenderName() != null)
                ? config.getSenderName() + " <" + config.getSmtpUsername() + ">"
                : config != null ? config.getSmtpUsername() : "noreply@pollen.com";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, html);
        mailSender.send(message);
    }
}
