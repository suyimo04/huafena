package com.pollen.management.controller;

import com.pollen.management.dto.AddPointsRequest;
import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.DeductPointsRequest;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 积分管理控制器：增减积分、查询记录、统计端点
 * 基础权限：ADMIN、LEADER、VICE_LEADER、MEMBER、INTERN（通过 SecurityConfig 配置）
 * 增减积分：仅 ADMIN、LEADER（通过 @PreAuthorize 限制）
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<PointsRecord> addPoints(@Valid @RequestBody AddPointsRequest request) {
        PointsRecord record = pointsService.addPoints(
                request.getUserId(), request.getPointsType(),
                request.getAmount(), request.getDescription());
        return ApiResponse.success(record);
    }

    @PostMapping("/deduct")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<PointsRecord> deductPoints(@Valid @RequestBody DeductPointsRequest request) {
        PointsRecord record = pointsService.deductPoints(
                request.getUserId(), request.getPointsType(),
                request.getAmount(), request.getDescription());
        return ApiResponse.success(record);
    }

    @GetMapping("/records/{userId}")
    public ApiResponse<List<PointsRecord>> getPointsRecords(@PathVariable Long userId) {
        List<PointsRecord> records = pointsService.getPointsRecords(userId);
        return ApiResponse.success(records);
    }

    @GetMapping("/total/{userId}")
    public ApiResponse<Integer> getTotalPoints(@PathVariable Long userId) {
        int total = pointsService.getTotalPoints(userId);
        return ApiResponse.success(total);
    }

    @GetMapping("/checkin-calculate")
    public ApiResponse<Integer> calculateCheckinPoints(@RequestParam int checkinCount) {
        int points = pointsService.calculateCheckinPoints(checkinCount);
        return ApiResponse.success(points);
    }

    @GetMapping("/convert")
    public ApiResponse<Integer> convertPointsToMiniCoins(@RequestParam int points) {
        int miniCoins = pointsService.convertPointsToMiniCoins(points);
        return ApiResponse.success(miniCoins);
    }
}
