package com.pollen.management.controller;

import com.pollen.management.dto.*;
import com.pollen.management.entity.Internship;
import com.pollen.management.entity.InternshipTask;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.InternshipStatus;
import com.pollen.management.repository.InternshipRepository;
import com.pollen.management.repository.InternshipTaskRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 实习期管理控制器：CRUD、任务管理、导师指派、进度查看、转正/延期/终止。
 * 权限：ADMIN/LEADER 管理操作，VICE_LEADER 只读访问。
 */
@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {

    private final InternshipService internshipService;
    private final InternshipRepository internshipRepository;
    private final InternshipTaskRepository internshipTaskRepository;
    private final UserRepository userRepository;

    /**
     * 创建实习记录（自动）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Internship> createInternship(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        Internship internship = internshipService.createForNewIntern(userId);
        return ApiResponse.success(internship);
    }

    /**
     * 实习记录列表
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<List<InternshipListItem>> listInternships(
            @RequestParam(required = false) InternshipStatus status) {
        List<Internship> internships;
        if (status != null) {
            internships = internshipRepository.findByStatus(status);
        } else {
            internships = internshipRepository.findAll();
        }

        // Collect all user IDs (intern + mentor) for batch lookup
        List<Long> userIds = internships.stream()
                .flatMap(i -> {
                    var ids = new java.util.ArrayList<Long>();
                    ids.add(i.getUserId());
                    if (i.getMentorId() != null) ids.add(i.getMentorId());
                    return ids.stream();
                })
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // Calculate task completion rates
        List<InternshipListItem> items = internships.stream().map(i -> {
            List<InternshipTask> tasks = internshipTaskRepository.findByInternshipId(i.getId());
            long total = tasks.size();
            long completed = tasks.stream().filter(InternshipTask::getCompleted).count();
            double rate = total == 0 ? 0.0 : (double) completed / total;

            User intern = userMap.get(i.getUserId());
            User mentor = i.getMentorId() != null ? userMap.get(i.getMentorId()) : null;

            return InternshipListItem.builder()
                    .id(i.getId())
                    .userId(i.getUserId())
                    .username(intern != null ? intern.getUsername() : "未知")
                    .mentorName(mentor != null ? mentor.getUsername() : null)
                    .startDate(i.getStartDate())
                    .expectedEndDate(i.getExpectedEndDate())
                    .status(i.getStatus())
                    .taskCompletionRate(rate)
                    .build();
        }).collect(Collectors.toList());

        return ApiResponse.success(items);
    }

    /**
     * 实习详情（含进度）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<Internship> getInternship(@PathVariable Long id) {
        return ApiResponse.success(internshipService.getById(id));
    }

    /**
     * 创建实习任务
     */
    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<InternshipTask> createTask(
            @PathVariable Long id,
            @Valid @RequestBody CreateInternshipTaskRequest request) {
        return ApiResponse.success(internshipService.createTask(id, request));
    }

    /**
     * 更新任务状态（完成）
     */
    @PutMapping("/{id}/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> completeTask(
            @PathVariable Long id,
            @PathVariable Long taskId) {
        internshipService.completeTask(id, taskId);
        return ApiResponse.success(null);
    }

    /**
     * 获取任务列表
     */
    @GetMapping("/{id}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<List<InternshipTask>> getTasks(@PathVariable Long id) {
        internshipService.getById(id); // validate exists
        return ApiResponse.success(internshipTaskRepository.findByInternshipId(id));
    }

    /**
     * 指派导师
     */
    @PutMapping("/{id}/mentor")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> assignMentor(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long mentorId = body.get("mentorId");
        internshipService.assignMentor(id, mentorId);
        return ApiResponse.success(null);
    }

    /**
     * 获取实习进度
     */
    @GetMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'VICE_LEADER')")
    public ApiResponse<InternshipProgress> getProgress(@PathVariable Long id) {
        return ApiResponse.success(internshipService.getProgress(id));
    }

    /**
     * 批准转正
     */
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> approveConversion(@PathVariable Long id) {
        internshipService.approveConversion(id);
        return ApiResponse.success(null);
    }

    /**
     * 延期实习
     */
    @PostMapping("/{id}/extend")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> extendInternship(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        int additionalDays = body.getOrDefault("additionalDays", 0);
        internshipService.extendInternship(id, additionalDays);
        return ApiResponse.success(null);
    }

    /**
     * 终止实习
     */
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> terminateInternship(@PathVariable Long id) {
        internshipService.terminateInternship(id);
        return ApiResponse.success(null);
    }
}
