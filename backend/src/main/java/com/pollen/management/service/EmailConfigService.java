package com.pollen.management.service;

import com.pollen.management.dto.UpdateEmailConfigRequest;
import com.pollen.management.entity.EmailConfig;

/**
 * SMTP 邮件配置服务：获取配置（脱敏）和更新配置。
 */
public interface EmailConfigService {

    /**
     * 获取当前 SMTP 配置，密码字段脱敏（用 **** 替代）。
     */
    EmailConfig getConfig();

    /**
     * 更新 SMTP 配置。密码通过 EncryptedStringConverter 自动加密存储。
     */
    void updateConfig(UpdateEmailConfigRequest request);
}
