package com.pollen.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateActivityRequest {

    @NotBlank(message = "活动名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "活动时间不能为空")
    private LocalDateTime eventTime;

    @NotBlank(message = "活动地点不能为空")
    private String location;

    @NotNull(message = "创建者ID不能为空")
    private Long createdBy;
}
