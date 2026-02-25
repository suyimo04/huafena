package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.AwardPointsRequest;
import com.pollen.management.dto.CreateActivityRequest;
import com.pollen.management.entity.Activity;
import com.pollen.management.entity.ActivityRegistration;
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
     * 创建活动
     * 仅 ADMIN、LEADER 可操作
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Activity> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        Activity activity = activityService.createActivity(
                request.getName(),
                request.getDescription(),
                request.getEventTime(),
                request.getLocation(),
                request.getCreatedBy());
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
     * 活动签到
     * 所有已认证用户可签到
     */
    @PostMapping("/{id}/check-in")
    public ApiResponse<ActivityRegistration> checkIn(
            @PathVariable Long id,
            @RequestParam Long userId) {
        ActivityRegistration registration = activityService.checkIn(id, userId);
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
}
