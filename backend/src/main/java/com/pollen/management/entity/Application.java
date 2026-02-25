package com.pollen.management.entity;

import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.entity.enums.ExamType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING_INITIAL_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    private Long questionnaireResponseId;

    private String reviewComment;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    // V3.1 新增字段：报名表单增强

    /** 花粉社区 UID（QQ号），5-11位纯数字，首位不为0 */
    @Column(length = 50)
    private String pollenUid;

    /** 出生年月 */
    private LocalDate birthDate;

    /** 根据出生年月自动计算的年龄 */
    private Integer calculatedAge;

    /** 教育阶段 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EducationStage educationStage;

    /** 中高考标识 */
    @Builder.Default
    private Boolean examFlag = false;

    /** 考试类型：中考/高考 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExamType examType;

    /** 考试日期 */
    private LocalDate examDate;

    /** 每周可用时段（JSON） */
    @Column(columnDefinition = "json")
    private String weeklyAvailableSlots;

    /** 每周可用天数 */
    private Integer weeklyAvailableDays;

    /** 每日可用时长 */
    @Column(precision = 4, scale = 1)
    private BigDecimal dailyAvailableHours;

    /** 自动筛选是否通过 */
    private Boolean screeningPassed;

    /** 自动筛选拒绝原因 */
    @Column(length = 200)
    private String screeningRejectReason;

    /** 是否需要人工重点审核 */
    @Builder.Default
    private Boolean needsAttention = false;

    /** 关注标记列表（JSON） */
    @Column(columnDefinition = "json")
    private String attentionFlags;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
