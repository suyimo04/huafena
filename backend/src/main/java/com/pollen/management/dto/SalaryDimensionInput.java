package com.pollen.management.dto;

import lombok.*;

/**
 * 维度录入输入 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryDimensionInput {
    private Long userId;
    private int communityActivityPoints;
    private int checkinCount;
    private int violationHandlingCount;
    private int taskCompletionPoints;
    private int announcementCount;
    private int eventHostingPoints;
    private int birthdayBonusPoints;
    private int monthlyExcellentPoints;
}
