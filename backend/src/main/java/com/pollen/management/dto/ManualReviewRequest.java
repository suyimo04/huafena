package com.pollen.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualReviewRequest {

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String reviewComment;

    private String suggestedMentor;

    private Long suggestedMentorId;
}
