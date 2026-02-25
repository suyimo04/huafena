package com.pollen.management.dto;

import com.pollen.management.entity.enums.ActivityType;
import com.pollen.management.entity.enums.ApprovalMode;
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

    /** V3.1: 活动封面图 URL */
    private String coverImageUrl;

    /** V3.1: 活动类型 */
    private ActivityType activityType;

    /** V3.1: 自定义报名表单字段 (JSON) */
    private String customFormFields;

    /** V3.1: 审核方式 (AUTO / MANUAL) */
    @Builder.Default
    private ApprovalMode approvalMode = ApprovalMode.AUTO;
}
