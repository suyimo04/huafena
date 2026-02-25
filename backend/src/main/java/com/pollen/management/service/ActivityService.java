package com.pollen.management.service;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityGroup;
import com.pollen.management.entity.ActivityRegistration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动管理服务接口
 */
public interface ActivityService {

    /**
     * 创建活动（名称、描述、时间、地点）
     */
    Activity createActivity(String name, String description, LocalDateTime eventTime, String location, Long createdBy);

    /**
     * 成员报名活动（支持额外字段和审核模式）
     */
    ActivityRegistration registerForActivity(Long activityId, Long userId);

    /**
     * 成员签到并发放签到奖励积分
     */
    ActivityRegistration checkIn(Long activityId, Long userId);

    /**
     * 归档活动（状态更新为 ARCHIVED）
     */
    Activity archiveActivity(Long activityId);

    /**
     * 查询活动列表
     */
    List<Activity> listActivities();

    /**
     * 举办活动根据评分发放 5-25 分积分奖励
     */
    void awardActivityPoints(Long activityId, Long userId, int score);

    /**
     * 人工审核模式下审批报名
     */
    void approveRegistration(Long activityId, Long registrationId);

    /**
     * 创建活动分组
     */
    ActivityGroup createGroup(Long activityId, String groupName, List<Long> memberIds);

    /**
     * 更新分组成员
     */
    void updateGroupMembers(Long activityId, Long groupId, List<Long> memberIds);

    /**
     * 获取活动分组列表
     */
    List<ActivityGroup> getGroups(Long activityId);
}

