package com.pollen.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "问卷回答ID不能为空")
    private Long questionnaireResponseId;

    /** V3.1: 报名表单数据（含花粉UID、出生年月、学生身份、可用性承诺） */
    @NotNull(message = "报名表单数据不能为空")
    @Valid
    private ApplicationFormData formData;
}
