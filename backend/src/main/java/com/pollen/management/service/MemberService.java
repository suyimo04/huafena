package com.pollen.management.service;

import com.pollen.management.dto.MemberCardItem;
import com.pollen.management.dto.MemberDetail;
import com.pollen.management.dto.RoleChangeRecord;
import com.pollen.management.dto.WeeklyActivityHour;
import com.pollen.management.entity.enums.OnlineStatus;

import java.util.List;

/**
 * 成员管理服务接口
 */
public interface MemberService {

    /**
     * 获取成员列表（卡片式数据）
     */
    List<MemberCardItem> listMembers();

    /**
     * 获取成员详情
     */
    MemberDetail getMemberDetail(Long id);

    /**
     * 获取成员每周活跃时长统计
     */
    List<WeeklyActivityHour> getActivityHours(Long memberId);

    /**
     * 获取成员角色变更历史
     */
    List<RoleChangeRecord> getRoleHistory(Long memberId);

    /**
     * 更新成员在线状态
     */
    void updateOnlineStatus(Long userId, OnlineStatus status);

    /**
     * 心跳接口（维持在线状态）
     */
    void heartbeat(Long userId);

    /**
     * 定时任务：30 分钟无操作设为离线
     */
    void checkAndUpdateOfflineStatus();
}
