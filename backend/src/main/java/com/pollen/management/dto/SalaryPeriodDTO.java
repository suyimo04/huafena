package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 薪酬周期 DTO
 * 包含周期标识、归档状态和记录数量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryPeriodDTO {

    /** 周期标识，格式 "YYYY-MM" */
    private String period;

    /** 该周期是否已归档 */
    private boolean archived;

    /** 该周期的记录数量 */
    private long recordCount;
}
