package com.pollen.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmailConfigRequest {

    @NotBlank(message = "SMTP 主机不能为空")
    private String smtpHost;

    @NotNull(message = "SMTP 端口不能为空")
    @Min(value = 1, message = "端口号最小为 1")
    @Max(value = 65535, message = "端口号最大为 65535")
    private Integer smtpPort;

    @NotBlank(message = "SMTP 用户名不能为空")
    private String smtpUsername;

    /** 密码字段：如果为空或 null 则不更新密码 */
    private String smtpPassword;

    private String senderName;

    @Builder.Default
    private Boolean sslEnabled = true;
}
