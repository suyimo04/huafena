package com.pollen.management.service;

import com.pollen.management.dto.EmailLogQueryRequest;
import com.pollen.management.dto.SendEmailRequest;
import com.pollen.management.entity.EmailLog;
import com.pollen.management.entity.EmailTemplate;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 邮件服务：发送邮件、模板邮件、查询模板和日志。
 */
public interface EmailService {

    /**
     * 发送自定义邮件（异步）。
     */
    void sendEmail(SendEmailRequest request);

    /**
     * 使用模板发送邮件（异步）。
     * 模板中的 {{variable}} 占位符会被 variables 中的值替换。
     */
    void sendTemplateEmail(String templateCode, Map<String, Object> variables, String recipient);

    /**
     * 获取所有邮件模板。
     */
    List<EmailTemplate> getTemplates();

    /**
     * 分页查询邮件发送日志。
     */
    Page<EmailLog> getLogs(EmailLogQueryRequest request);
}
