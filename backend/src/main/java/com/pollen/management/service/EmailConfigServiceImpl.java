package com.pollen.management.service;

import com.pollen.management.dto.UpdateEmailConfigRequest;
import com.pollen.management.entity.EmailConfig;
import com.pollen.management.repository.EmailConfigRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailConfigServiceImpl implements EmailConfigService {

    private final EmailConfigRepository emailConfigRepository;

    private static final String MASKED_PASSWORD = "****";

    @Override
    public EmailConfig getConfig() {
        EmailConfig config = emailConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "SMTP 配置不存在"));
        // 脱敏：将密码替换为 ****
        config.setSmtpPasswordEncrypted(MASKED_PASSWORD);
        return config;
    }

    @Override
    @Transactional
    public void updateConfig(UpdateEmailConfigRequest request) {
        EmailConfig config = emailConfigRepository.findAll().stream()
                .findFirst()
                .orElse(EmailConfig.builder().build());

        config.setSmtpHost(request.getSmtpHost());
        config.setSmtpPort(request.getSmtpPort());
        config.setSmtpUsername(request.getSmtpUsername());
        config.setSenderName(request.getSenderName());
        config.setSslEnabled(request.getSslEnabled());

        // 仅当密码非空且非脱敏占位符时才更新密码
        if (request.getSmtpPassword() != null
                && !request.getSmtpPassword().isBlank()
                && !MASKED_PASSWORD.equals(request.getSmtpPassword())) {
            config.setSmtpPasswordEncrypted(request.getSmtpPassword());
        }

        emailConfigRepository.save(config);
    }
}
