package com.pollen.management.dto;

import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V3.1 申请详情 DTO，包含所有增强字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDetailDTO {

    private Long id;
    private Long userId;
    private ApplicationStatus status;
    private EntryType entryType;
    private Long questionnaireResponseId;
    private String reviewComment;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    // V3.1 增强字段
    private String pollenUid;
    private LocalDate birthDate;
    private Integer calculatedAge;
    private EducationStage educationStage;
    private Boolean examFlag;
    private ExamType examType;
    private LocalDate examDate;
    private List<String> weeklyAvailableSlots;
    private Integer weeklyAvailableDays;
    private BigDecimal dailyAvailableHours;
    private Boolean screeningPassed;
    private String screeningRejectReason;
    private Boolean needsAttention;
    private List<String> attentionFlags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
