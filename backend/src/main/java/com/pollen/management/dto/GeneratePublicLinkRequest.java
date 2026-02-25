package com.pollen.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratePublicLinkRequest {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;
}
