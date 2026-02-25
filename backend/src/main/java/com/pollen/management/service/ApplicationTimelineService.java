package com.pollen.management.service;

import com.pollen.management.entity.ApplicationTimeline;

import java.util.List;

/**
 * 申请流程时间线服务：记录申请状态变更事件，查询时间线。
 */
public interface ApplicationTimelineService {

    /**
     * 记录时间线事件。
     *
     * @param applicationId 申请 ID
     * @param status        状态节点
     * @param operator      操作人
     * @param description   描述
     */
    void recordTimelineEvent(Long applicationId, String status, String operator, String description);

    /**
     * 获取申请的完整时间线，按时间正序排列。
     *
     * @param applicationId 申请 ID
     * @return 时间线节点列表
     */
    List<ApplicationTimeline> getTimeline(Long applicationId);
}
