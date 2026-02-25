package com.pollen.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwardPointsRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "评分不能为空")
    @Min(value = 5, message = "活动积分奖励范围为 5-25 分")
    @Max(value = 25, message = "活动积分奖励范围为 5-25 分")
    private Integer score;
}
