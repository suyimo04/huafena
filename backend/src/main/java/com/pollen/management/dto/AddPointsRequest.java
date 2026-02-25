package com.pollen.management.dto;

import com.pollen.management.entity.enums.PointsType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPointsRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "pointsType 不能为空")
    private PointsType pointsType;

    @NotNull(message = "amount 不能为空")
    @Positive(message = "积分数额必须为正数")
    private Integer amount;

    private String description;
}
