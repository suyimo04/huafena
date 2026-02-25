package com.pollen.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartInterviewRequest {

    @NotNull(message = "applicationId 不能为空")
    private Long applicationId;

    @NotNull(message = "scenarioId 不能为空")
    private String scenarioId;
}
