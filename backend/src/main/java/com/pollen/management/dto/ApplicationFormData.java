package com.pollen.management.dto;

import com.pollen.management.entity.enums.EducationStage;
import com.pollen.management.entity.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * V3.1 报名表单数据 DTO，包含花粉 UID、出生年月、学生身份、可用性承诺等字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationFormData {

    /** 花粉社区 UID（QQ号），5-11位纯数字，首位不为0 */
    @NotBlank(message = "花粉社区UID不能为空")
    private String pollenUid;

    /** 出生年月 */
    @NotNull(message = "出生年月不能为空")
    private LocalDate birthDate;

    /** 教育阶段 */
    @NotNull(message = "教育阶段不能为空")
    private EducationStage educationStage;

    /** 中高考标识 */
    private Boolean examFlag;

    /** 考试类型：中考/高考 */
    private ExamType examType;

    /** 考试日期 */
    private LocalDate examDate;

    /** 每周可用时段 */
    private List<String> weeklyAvailableSlots;

    /** 每周可用天数 */
    private Integer weeklyAvailableDays;

    /** 每日可用时长 */
    private BigDecimal dailyAvailableHours;
}
