package com.pollen.management.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.ApiResponse;
import com.pollen.management.dto.CheckinTier;
import com.pollen.management.service.SalaryConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 薪资配置管理控制器：获取/更新薪酬配置参数和签到奖惩表。
 * 读操作：已认证用户可访问
 * 写操作：仅 ADMIN/LEADER 可修改
 */
@RestController
@RequestMapping("/api/salary-config")
@RequiredArgsConstructor
public class SalaryConfigController {

    private final SalaryConfigService salaryConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有薪资配置项
     */
    @GetMapping
    public ApiResponse<Map<String, String>> getAllConfig() {
        return ApiResponse.success(salaryConfigService.getAllConfig());
    }

    /**
     * 批量更新薪资配置（含校验）
     * 仅 ADMIN/LEADER 可操作
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> updateConfig(@RequestBody Map<String, String> configMap) {
        salaryConfigService.saveConfig(configMap);
        return ApiResponse.success(null);
    }

    /**
     * 获取签到奖惩分级表
     */
    @GetMapping("/checkin-tiers")
    public ApiResponse<List<CheckinTier>> getCheckinTiers() {
        return ApiResponse.success(salaryConfigService.getCheckinTiers());
    }

    /**
     * 更新签到奖惩分级表
     * 仅 ADMIN/LEADER 可操作
     */
    @PutMapping("/checkin-tiers")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public ApiResponse<Void> updateCheckinTiers(@RequestBody List<CheckinTier> tiers) {
        try {
            String json = objectMapper.writeValueAsString(tiers);
            salaryConfigService.saveConfig(Map.of("checkin_tiers", json));
            return ApiResponse.success(null);
        } catch (JsonProcessingException e) {
            return ApiResponse.error(400, "签到奖惩表序列化失败: " + e.getMessage());
        }
    }
}
