package com.pollen.management.controller;

import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.ExecutePromotionRequest;
import com.pollen.management.entity.User;
import com.pollen.management.service.MemberRotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员流转管理控制器
 * 提供转正评议触发、角色流转执行、待开除列表查询等端点
 * 权限：ADMIN 和 LEADER 可访问（通过 SecurityConfig 配置）
 */
@RestController
@RequestMapping("/api/member-rotation")
@RequiredArgsConstructor
public class MemberRotationController {

    private final MemberRotationService memberRotationService;

    /**
     * 检查符合转正条件的实习成员
     * POST /api/member-rotation/check-promotion
     */
    @PostMapping("/check-promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<User>> checkPromotionEligibility() {
        List<User> eligible = memberRotationService.checkPromotionEligibility();
        return ApiResponse.success(eligible);
    }

    /**
     * 检查薪酬不达标的正式成员（降级候选人）
     * POST /api/member-rotation/check-demotion
     */
    @PostMapping("/check-demotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<User>> checkDemotionCandidates() {
        List<User> candidates = memberRotationService.checkDemotionCandidates();
        return ApiResponse.success(candidates);
    }

    /**
     * 触发转正评议流程
     * POST /api/member-rotation/trigger-review
     */
    @PostMapping("/trigger-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Boolean> triggerPromotionReview() {
        boolean triggered = memberRotationService.triggerPromotionReview();
        return ApiResponse.success(triggered);
    }

    /**
     * 执行角色流转（管理组评议通过后调用）
     * POST /api/member-rotation/execute
     */
    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> executePromotion(@Valid @RequestBody ExecutePromotionRequest request) {
        memberRotationService.executePromotion(request.getInternId(), request.getMemberId());
        return ApiResponse.success(null);
    }

    /**
     * 标记待开除的实习成员
     * POST /api/member-rotation/mark-dismissal
     */
    @PostMapping("/mark-dismissal")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<User>> markForDismissal() {
        List<User> marked = memberRotationService.markForDismissal();
        return ApiResponse.success(marked);
    }

    /**
     * 获取待开除实习成员列表
     * GET /api/member-rotation/pending-dismissal
     */
    @GetMapping("/pending-dismissal")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<List<User>> getPendingDismissalList() {
        List<User> pendingList = memberRotationService.getPendingDismissalList();
        return ApiResponse.success(pendingList);
    }
}
