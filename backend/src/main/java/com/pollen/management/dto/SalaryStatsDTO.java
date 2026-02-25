package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 薪酬数据统计 DTO
 * 薪酬池使用情况、成员薪酬排行榜
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStatsDTO {

    /** 薪酬池总额（2000 迷你币） */
    private int totalPool;

    /** 已分配迷你币 */
    private int allocated;

    /** 使用率（0.0 ~ 1.0） */
    private double usageRate;

    /** 成员薪酬排行榜（按迷你币降序） */
    private List<MemberSalaryRank> ranking;
}
