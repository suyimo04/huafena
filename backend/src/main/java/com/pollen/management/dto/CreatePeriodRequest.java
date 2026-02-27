package com.pollen.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建薪酬周期请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePeriodRequest {

    @NotBlank(message = "周期不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "周期格式必须为 YYYY-MM")
    private String period;
}
