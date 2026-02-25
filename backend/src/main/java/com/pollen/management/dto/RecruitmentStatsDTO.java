package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 招募数据统计 DTO
 * 各招募阶段人数、AI 面试通过率、人工复审通过率
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentStatsDTO {

    /** 各招募阶段人数 */
    private Map<String, Long> stageCount;

    /** AI 面试通过率（0.0 ~ 1.0） */
    private double aiInterviewPassRate;

    /** 人工复审通过率（0.0 ~ 1.0） */
    private double manualReviewPassRate;
}
