package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.AwardPointsRequest;
import com.pollen.management.dto.CreateActivityRequest;
import com.pollen.management.dto.CreateGroupRequest;
import com.pollen.management.dto.FeedbackRequest;
import com.pollen.management.dto.UpdateGroupMembersRequest;
import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityFeedback;
import com.pollen.management.entity.ActivityGroup;
import com.pollen.management.entity.ActivityMaterial;
import com.pollen.management.entity.ActivityRegistration;
import com.pollen.management.entity.ActivityStatistics;
import com.pollen.management.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动管理控制器：创建、报名、签到、归档、列表查询、积分奖励端点
 * 基础权限：ADMIN、LEADER、VICE_LEADER、MEMBER、INTERN（通过 SecurityConfig 配置）
 * 创建/归档/奖励积分：仅 ADMIN、LEADER（通过 @PreAuthorize 限制）
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 创建活动（V3.1 增强：含封面图、类型、报名表单、审核方式）
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Activity> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        Activity activity = activityService.createActivity(request);
        return ApiResponse.success(activity);
    }

    /**
     * 查询活动列表
     * 所有已认证用户可查看
     */
    @GetMapping
    public ApiResponse<List<Activity>> listActivities() {
        List<Activity> activities = activityService.listActivities();
        return ApiResponse.success(activities);
    }

    /**
     * 报名活动
     * 所有已认证用户可报名
     */
    @PostMapping("/{id}/register")
    public ApiResponse<ActivityRegistration> registerForActivity(
            @PathVariable Long id,
            @RequestParam Long userId) {
        ActivityRegistration registration = activityService.registerForActivity(id, userId);
        return ApiResponse.success(registration);
    }

    /**
     * 活动签到（支持可选的二维码 token）
     * 所有已认证用户可签到
     */
    @PostMapping("/{id}/check-in")
    public ApiResponse<ActivityRegistration> checkIn(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(required = false) String qrToken) {
        ActivityRegistration registration = activityService.checkIn(id, userId, qrToken);
        return ApiResponse.success(registration);
    }

    /**
     * 归档活动
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Activity> archiveActivity(@PathVariable Long id) {
        Activity activity = activityService.archiveActivity(id);
        return ApiResponse.success(activity);
    }

    /**
     * 发放活动积分奖励
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping("/{id}/award-points")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> awardActivityPoints(
            @PathVariable Long id,
            @Valid @RequestBody AwardPointsRequest request) {
        activityService.awardActivityPoints(id, request.getUserId(), request.getScore());
        return ApiResponse.success(null);
    }

    /**
     * 审批报名（人工审核模式）
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping("/{id}/registrations/{regId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> approveRegistration(
            @PathVariable Long id,
            @PathVariable Long regId) {
        activityService.approveRegistration(id, regId);
        return ApiResponse.success(null);
    }

    /**
     * 创建活动分组
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping("/{id}/groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<ActivityGroup> createGroup(
            @PathVariable Long id,
            @Valid @RequestBody CreateGroupRequest request) {
        ActivityGroup group = activityService.createGroup(id, request.getGroupName(), request.getMemberIds());
        return ApiResponse.success(group);
    }

    /**
     * 更新分组成员
     * 仅 ADMIN、LEADER 可操作
     */
    @PutMapping("/{id}/groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> updateGroupMembers(
            @PathVariable Long id,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupMembersRequest request) {
        activityService.updateGroupMembers(id, groupId, request.getMemberIds());
        return ApiResponse.success(null);
    }

    /**
     * 获取活动分组列表
     */
    @GetMapping("/{id}/groups")
    public ApiResponse<List<ActivityGroup>> getGroups(@PathVariable Long id) {
        List<ActivityGroup> groups = activityService.getGroups(id);
        return ApiResponse.success(groups);
    }

    /**
     * 生成活动签到二维码
     * 仅 ADMIN、LEADER 可操作
     */
    @GetMapping("/{id}/qr-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<String> generateQrCode(@PathVariable Long id) {
        String qrToken = activityService.generateQrCode(id);
        return ApiResponse.success(qrToken);
    }

    /**
     * 提交活动反馈
     * 所有已认证用户可提交
     */
    @PostMapping("/{id}/feedback")
    public ApiResponse<Void> submitFeedback(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody FeedbackRequest request) {
        activityService.submitFeedback(id, userId, request);
        return ApiResponse.success(null);
    }

    /**
     * 获取活动反馈列表
     */
    @GetMapping("/{id}/feedback")
    public ApiResponse<List<ActivityFeedback>> getFeedback(@PathVariable Long id) {
        List<ActivityFeedback> feedback = activityService.getFeedback(id);
        return ApiResponse.success(feedback);
    }

    /**
     * 获取活动统计数据
     */
    @GetMapping("/{id}/statistics")
    public ApiResponse<ActivityStatistics> getStatistics(@PathVariable Long id) {
        ActivityStatistics statistics = activityService.getStatistics(id);
        return ApiResponse.success(statistics);
    }

    /**
     * 上传活动资料
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping("/{id}/materials")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> uploadMaterial(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam Long uploadedBy) {
        activityService.uploadMaterial(id, file, uploadedBy);
        return ApiResponse.success(null);
    }

    /**
     * 获取活动资料列表
     */
    @GetMapping("/{id}/materials")
    public ApiResponse<List<ActivityMaterial>> getMaterials(@PathVariable Long id) {
        List<ActivityMaterial> materials = activityService.getMaterials(id);
        return ApiResponse.success(materials);
    }
}
