package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据看板统计 DTO
 * 显示成员总数、各角色人数、活动数量、积分统计等关键指标
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    /** 成员总数 */
    private long totalMembers;

    /** 管理员人数 */
    private long adminCount;

    /** 组长人数 */
    private long leaderCount;

    /** 副组长人数 */
    private long viceLeaderCount;

    /** 正式成员人数 */
    private long memberCount;

    /** 实习成员人数 */
    private long internCount;

    /** 申请者人数 */
    private long applicantCount;

    /** 活动总数 */
    private long totalActivities;

    /** 积分记录总数 */
    private long totalPointsRecords;
}
