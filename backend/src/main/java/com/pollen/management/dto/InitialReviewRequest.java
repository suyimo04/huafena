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
public class InitialReviewRequest {

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
}
