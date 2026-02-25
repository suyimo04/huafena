package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每周活跃时长统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyActivityHour {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private int totalMinutes;
}
