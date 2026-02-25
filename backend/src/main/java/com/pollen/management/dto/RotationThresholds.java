package com.pollen.management.dto;

import lombok.*;

/**
 * 流转阈值配置 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RotationThresholds {
    private int promotionPointsThreshold;
    private int demotionSalaryThreshold;
    private int demotionConsecutiveMonths;
    private int dismissalPointsThreshold;
    private int dismissalConsecutiveMonths;
}
