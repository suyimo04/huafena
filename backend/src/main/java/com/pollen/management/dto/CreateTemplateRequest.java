package com.pollen.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemplateRequest {

    @NotBlank(message = "问卷标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "问卷配置不能为空")
    private String schemaDefinition;
}
