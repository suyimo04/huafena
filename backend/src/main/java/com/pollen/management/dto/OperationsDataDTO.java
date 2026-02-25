package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 运营数据看板 DTO
 * 用户增长趋势（按月统计）、问题处理效率统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationsDataDTO {

    /** 用户增长趋势（按月统计新增用户数，最近12个月） */
    private List<MonthlyGrowthDTO> userGrowthTrend;

    /** 问题处理效率统计：totalApplications, processedApplications, processingRate */
    private Map<String, Object> issueProcessingStats;
}
