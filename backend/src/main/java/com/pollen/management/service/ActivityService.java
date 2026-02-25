package com.pollen.management.service;

import com.pollen.management.dto.CreateActivityRequest;
import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityFeedback;
import com.pollen.management.entity.ActivityGroup;
import com.pollen.management.entity.ActivityMaterial;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.ActivityStatistics;
import com.pollen.management.dto.FeedbackRequest;
import org.springframework.web.multipart.MultipartFile;

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
     * V3.1: 创建活动（含封面图、类型、报名表单、审核方式）
     */
    Activity createActivity(CreateActivityRequest request);

    /**
     * 成员报名活动（支持额外字段和审核模式）
     */
    ActivityRegistration registerForActivity(Long activityId, Long userId);

    /**
     * 成员签到（无 QR token）
     */
    ActivityRegistration checkIn(Long activityId, Long userId);

    /**
     * 成员签到（支持 QR token 验证）
     */
    ActivityRegistration checkIn(Long activityId, Long userId, String qrToken);

    /**
     * 生成活动签到二维码 token
     */
    String generateQrCode(Long activityId);

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

    /**
     * 提交活动反馈
     */
    void submitFeedback(Long activityId, Long userId, FeedbackRequest request);

    /**
     * 获取活动反馈列表
     */
    List<ActivityFeedback> getFeedback(Long activityId);

    /**
     * 获取活动统计数据（报名人数、实际参与人数、签到率、反馈汇总）
     */
    ActivityStatistics getStatistics(Long activityId);

    /**
     * 上传活动资料
     */
    void uploadMaterial(Long activityId, MultipartFile file, Long uploadedBy);

    /**
     * 获取活动资料列表
     */
    List<ActivityMaterial> getMaterials(Long activityId);
}

