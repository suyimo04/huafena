package com.pollen.management.service;

import com.pollen.management.entity.EmailConfig;
import com.pollen.management.repository.EmailConfigRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private EmailConfigRepository emailConfigRepository;

    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailSender = new EmailSender(emailConfigRepository);
    }

    @Test
    void createMailSender_shouldThrowWhenNoConfig() {
        when(emailConfigRepository.findAll()).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> emailSender.createMailSender());
    }

    @Test
    void createMailSender_shouldCreateSenderWithSsl() {
        EmailConfig config = EmailConfig.builder()
                .smtpHost("smtp.qq.com").smtpPort(465)
                .smtpUsername("test@qq.com").smtpPasswordEncrypted("pass")
                .senderName("花粉小组").sslEnabled(true).build();
        when(emailConfigRepository.findAll()).thenReturn(List.of(config));

        assertNotNull(emailSender.createMailSender());
    }

    @Test
    void createMailSender_shouldCreateSenderWithStartTls() {
        EmailConfig config = EmailConfig.builder()
                .smtpHost("smtp.gmail.com").smtpPort(587)
                .smtpUsername("test@gmail.com").smtpPasswordEncrypted("pass")
                .senderName("Pollen").sslEnabled(false).build();
        when(emailConfigRepository.findAll()).thenReturn(List.of(config));

        assertNotNull(emailSender.createMailSender());
    }

    @Test
    void doSendEmail_shouldThrowWhenNoConfig() {
        when(emailConfigRepository.findAll()).thenReturn(List.of());
        assertThrows(BusinessException.class, () ->
                emailSender.doSendEmail("test@example.com", "Subject", "<p>Body</p>", true));
    }
}
