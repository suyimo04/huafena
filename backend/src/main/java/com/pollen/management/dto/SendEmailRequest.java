package com.pollen.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEmailRequest {

    @NotBlank(message = "收件人不能为空")
    @Email(message = "收件人邮箱格式不正确")
    private String recipient;

    @NotBlank(message = "邮件主题不能为空")
    private String subject;

    @NotBlank(message = "邮件内容不能为空")
    private String body;

    /** 是否为 HTML 格式 */
    @Builder.Default
    private boolean html = true;
}
