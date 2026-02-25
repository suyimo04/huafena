package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度用户增长数据项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyGrowthDTO {

    /** 月份标识，格式 yyyy-MM */
    private String month;

    /** 该月新增用户数 */
    private long count;
}
