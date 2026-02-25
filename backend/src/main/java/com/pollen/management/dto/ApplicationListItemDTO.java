package com.pollen.management.dto;

import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.EntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * V3.1 申请列表项 DTO，用于列表展示（含中高考高亮标记等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationListItemDTO {

    private Long id;
    private Long userId;
    private ApplicationStatus status;
    private EntryType entryType;
    private String pollenUid;
    private Integer calculatedAge;
    private EducationStage educationStage;
    private Boolean examFlag;
    private Boolean needsAttention;
    private Boolean screeningPassed;
    private LocalDateTime createdAt;
}
