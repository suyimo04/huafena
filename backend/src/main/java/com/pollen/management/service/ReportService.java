package com.pollen.management.service;

import com.pollen.management.entity.WeeklyReport;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表服务接口
 * 负责周报自动生成、查询
 */
public interface ReportService {

    /**
     * 生成指定周的周报
     * @param weekStart 周开始日期（周一）
     * @param weekEnd 周结束日期（周日）
     * @return 生成的周报
     */
    WeeklyReport generateWeeklyReport(LocalDate weekStart, LocalDate weekEnd);

    /**
     * 获取周报列表，按 weekStart 降序排列
     * @return 周报列表
     */
    List<WeeklyReport> listWeeklyReports();

    /**
     * 根据 ID 获取周报详情
     * @param id 周报 ID
     * @return 周报
     */
    WeeklyReport getWeeklyReport(Long id);
}
