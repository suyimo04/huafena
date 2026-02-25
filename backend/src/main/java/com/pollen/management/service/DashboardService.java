package com.pollen.management.service;

import com.pollen.management.dto.DashboardStatsDTO;

/**
 * 数据看板服务接口
 */
public interface DashboardService {

    /**
     * 获取数据看板统计指标
     * 包含成员总数、各角色人数、活动数量、积分统计等
     */
    DashboardStatsDTO getDashboardStats();
}
