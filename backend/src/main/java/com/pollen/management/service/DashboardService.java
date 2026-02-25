package com.pollen.management.service;

import com.pollen.management.dto.DashboardStatsDTO;
import com.pollen.management.dto.OperationsDataDTO;
import com.pollen.management.dto.RecruitmentStatsDTO;
import com.pollen.management.dto.SalaryStatsDTO;

/**
 * 数据看板服务接口
 */
public interface DashboardService {

    /**
     * 获取数据看板统计指标
     * 包含成员总数、各角色人数、活动数量、积分统计等
     */
    DashboardStatsDTO getDashboardStats();

    /**
     * 获取招募数据统计
     * 各招募阶段人数、AI 面试通过率、人工复审通过率
     */
    RecruitmentStatsDTO getRecruitmentStats();

    /**
     * 获取薪酬数据统计
     * 薪酬池使用情况、成员薪酬排行榜
     */
    SalaryStatsDTO getSalaryStats();

    /**
     * 获取运营数据
     * 用户增长趋势图（按月统计）、问题处理效率统计
     */
    OperationsDataDTO getOperationsData();
}

