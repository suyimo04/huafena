package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.enums.OnlineStatus;
import com.pollen.management.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员管理控制器：成员列表、详情、活跃时长、角色历史、状态更新、心跳端点
 * 权限：ADMIN、LEADER、VICE_LEADER 可访问成员管理
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 获取成员列表（卡片式数据）
     * GET /api/members
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<List<MemberCardItem>> listMembers() {
        List<MemberCardItem> members = memberService.listMembers();
        return ApiResponse.success(members);
    }

    /**
     * 获取成员详情
     * GET /api/members/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<MemberDetail> getMemberDetail(@PathVariable Long id) {
        MemberDetail detail = memberService.getMemberDetail(id);
        return ApiResponse.success(detail);
    }

    /**
     * 获取成员每周活跃时长
     * GET /api/members/{id}/activity-hours
     */
    @GetMapping("/{id}/activity-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<List<WeeklyActivityHour>> getActivityHours(@PathVariable Long id) {
        List<WeeklyActivityHour> hours = memberService.getActivityHours(id);
        return ApiResponse.success(hours);
    }

    /**
     * 获取成员角色变更历史
     * GET /api/members/{id}/role-history
     */
    @GetMapping("/{id}/role-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<List<RoleChangeRecord>> getRoleHistory(@PathVariable Long id) {
        List<RoleChangeRecord> history = memberService.getRoleHistory(id);
        return ApiResponse.success(history);
    }

    /**
     * 更新成员在线状态
     * PUT /api/members/{id}/status
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateOnlineStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOnlineStatusRequest request) {
        memberService.updateOnlineStatus(id, request.getStatus());
        return ApiResponse.success(null);
    }

    /**
     * 心跳接口（维持在线状态）
     * POST /api/members/{id}/heartbeat
     */
    @PostMapping("/{id}/heartbeat")
    public ApiResponse<Void> heartbeat(@PathVariable Long id) {
        memberService.heartbeat(id);
        return ApiResponse.success(null);
    }
}
