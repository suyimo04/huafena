package com.pollen.management.service;

import com.pollen.management.dto.UpdateEmailConfigRequest;
import com.pollen.management.entity.EmailConfig;
import com.pollen.management.repository.EmailConfigRepository;
import com.pollen.management.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigServiceImplTest {

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @InjectMocks
    private EmailConfigServiceImpl service;

    @Test
    void getConfig_shouldReturnConfigWithMaskedPassword() {
        var config = EmailConfig.builder()
                .id(1L)
                .smtpHost("smtp.qq.com")
                .smtpPort(465)
                .smtpUsername("admin@qq.com")
                .smtpPasswordEncrypted("realpassword")
                .senderName("花粉系统")
                .sslEnabled(true)
                .build();
        when(emailConfigRepository.findAll()).thenReturn(List.of(config));

        EmailConfig result = service.getConfig();

        assertThat(result.getSmtpHost()).isEqualTo("smtp.qq.com");
        assertThat(result.getSmtpPasswordEncrypted()).isEqualTo("****");
    }

    @Test
    void getConfig_noConfig_shouldThrowException() {
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getConfig())
                .isInstanceOf(BusinessException.class)
                .hasMessage("SMTP 配置不存在");
    }

    @Test
    void updateConfig_shouldUpdateAllFieldsIncludingPassword() {
        var existing = EmailConfig.builder()
                .id(1L)
                .smtpHost("old.host.com")
                .smtpPort(25)
                .smtpUsername("old@test.com")
                .smtpPasswordEncrypted("oldpass")
                .senderName("Old Name")
                .sslEnabled(false)
                .build();
        when(emailConfigRepository.findAll()).thenReturn(List.of(existing));
        when(emailConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = UpdateEmailConfigRequest.builder()
                .smtpHost("smtp.163.com")
                .smtpPort(465)
                .smtpUsername("new@163.com")
                .smtpPassword("newpassword")
                .senderName("新名称")
                .sslEnabled(true)
                .build();

        service.updateConfig(request);

        ArgumentCaptor<EmailConfig> captor = ArgumentCaptor.forClass(EmailConfig.class);
        verify(emailConfigRepository).save(captor.capture());
        EmailConfig saved = captor.getValue();
        assertThat(saved.getSmtpHost()).isEqualTo("smtp.163.com");
        assertThat(saved.getSmtpPort()).isEqualTo(465);
        assertThat(saved.getSmtpUsername()).isEqualTo("new@163.com");
        assertThat(saved.getSmtpPasswordEncrypted()).isEqualTo("newpassword");
        assertThat(saved.getSenderName()).isEqualTo("新名称");
        assertThat(saved.getSslEnabled()).isTrue();
    }

    @Test
    void updateConfig_withMaskedPassword_shouldNotUpdatePassword() {
        var existing = EmailConfig.builder()
                .id(1L)
                .smtpHost("smtp.qq.com")
                .smtpPort(465)
                .smtpUsername("admin@qq.com")
                .smtpPasswordEncrypted("originalpass")
                .senderName("花粉系统")
                .sslEnabled(true)
                .build();
        when(emailConfigRepository.findAll()).thenReturn(List.of(existing));
        when(emailConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = UpdateEmailConfigRequest.builder()
                .smtpHost("smtp.qq.com")
                .smtpPort(465)
                .smtpUsername("admin@qq.com")
                .smtpPassword("****")
                .senderName("花粉系统")
                .sslEnabled(true)
                .build();

        service.updateConfig(request);

        ArgumentCaptor<EmailConfig> captor = ArgumentCaptor.forClass(EmailConfig.class);
        verify(emailConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getSmtpPasswordEncrypted()).isEqualTo("originalpass");
    }

    @Test
    void updateConfig_withNullPassword_shouldNotUpdatePassword() {
        var existing = EmailConfig.builder()
                .id(1L)
                .smtpHost("smtp.qq.com")
                .smtpPort(465)
                .smtpUsername("admin@qq.com")
                .smtpPasswordEncrypted("keepthis")
                .senderName("花粉系统")
                .sslEnabled(true)
                .build();
        when(emailConfigRepository.findAll()).thenReturn(List.of(existing));
        when(emailConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = UpdateEmailConfigRequest.builder()
                .smtpHost("smtp.qq.com")
                .smtpPort(465)
                .smtpUsername("admin@qq.com")
                .smtpPassword(null)
                .senderName("花粉系统")
                .sslEnabled(true)
                .build();

        service.updateConfig(request);

        ArgumentCaptor<EmailConfig> captor = ArgumentCaptor.forClass(EmailConfig.class);
        verify(emailConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getSmtpPasswordEncrypted()).isEqualTo("keepthis");
    }

    @Test
    void updateConfig_noExistingConfig_shouldCreateNew() {
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());
        when(emailConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = UpdateEmailConfigRequest.builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .smtpUsername("user@gmail.com")
                .smtpPassword("gmailpass")
                .senderName("Gmail Sender")
                .sslEnabled(true)
                .build();

        service.updateConfig(request);

        ArgumentCaptor<EmailConfig> captor = ArgumentCaptor.forClass(EmailConfig.class);
        verify(emailConfigRepository).save(captor.capture());
        EmailConfig saved = captor.getValue();
        assertThat(saved.getSmtpHost()).isEqualTo("smtp.gmail.com");
        assertThat(saved.getSmtpPasswordEncrypted()).isEqualTo("gmailpass");
    }
}
