package com.pollen.management.dto;

import lombok.*;

/**
 * 签到奖惩分级 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckinTier {
    private int minCount;
    private int maxCount;
    private int points;
    private String label;
}
