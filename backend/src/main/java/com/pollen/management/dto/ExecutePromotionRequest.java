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
public class ExecutePromotionRequest {

    @NotNull(message = "实习成员ID不能为空")
    private Long internId;

    @NotNull(message = "正式成员ID不能为空")
    private Long memberId;
}
