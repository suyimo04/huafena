package com.pollen.management.dto;

import lombok.*;

/**
 * 薪资计算结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryCalculationResult {
    private int basePoints;
    private int bonusPoints;
    private int totalPoints;
    private int miniCoins;
    private int checkinPoints;
    private int violationHandlingPoints;
    private int announcementPoints;
    private String checkinLevel;
}
