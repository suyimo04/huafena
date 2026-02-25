package com.pollen.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.RotationThresholds;
import com.pollen.management.entity.SalaryConfig;
import com.pollen.management.repository.SalaryConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 薪资配置管理服务实现
 */
@Service
@RequiredArgsConstructor
public class SalaryConfigServiceImpl implements SalaryConfigService {

    private final SalaryConfigRepository salaryConfigRepository;
    private final ObjectMapper objectMapper;

    // 默认配置值
    static final int DEFAULT_SALARY_POOL_TOTAL = 2000;
    static final int DEFAULT_FORMAL_MEMBER_COUNT = 5;
    static final int DEFAULT_BASE_ALLOCATION = 400;
    static final int DEFAULT_MINI_COINS_MIN = 200;
    static final int DEFAULT_MINI_COINS_MAX = 400;
    static final int DEFAULT_POINTS_TO_COINS_RATIO = 2;
    static final int DEFAULT_PROMOTION_POINTS_THRESHOLD = 100;
    static final int DEFAULT_DEMOTION_SALARY_THRESHOLD = 150;
    static final int DEFAULT_DEMOTION_CONSECUTIVE_MONTHS = 2;
    static final int DEFAULT_DISMISSAL_POINTS_THRESHOLD = 100;
    static final int DEFAULT_DISMISSAL_CONSECUTIVE_MONTHS = 2;

    static final String DEFAULT_CHECKIN_TIERS_JSON = "[" +
            "{\"minCount\":0,\"maxCount\":19,\"points\":-20,\"label\":\"不合格\"}," +
            "{\"minCount\":20,\"maxCount\":29,\"points\":-10,\"label\":\"需改进\"}," +
            "{\"minCount\":30,\"maxCount\":39,\"points\":0,\"label\":\"合格\"}," +
            "{\"minCount\":40,\"maxCount\":49,\"points\":30,\"label\":\"良好\"}," +
            "{\"minCount\":50,\"maxCount\":999,\"points\":50,\"label\":\"优秀\"}" +
            "]";

    @Override
    public Map<String, String> getAllConfig() {
        List<SalaryConfig> configs = salaryConfigRepository.findAll();
        Map<String, String> result = new HashMap<>();
        for (SalaryConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        return salaryConfigRepository.findByConfigKey(key)
                .map(SalaryConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        return salaryConfigRepository.findByConfigKey(key)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional
    public void saveConfig(Map<String, String> configMap) {
        validateConfig(configMap);

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            SalaryConfig config = salaryConfigRepository.findByConfigKey(entry.getKey())
                    .orElse(SalaryConfig.builder()
                            .configKey(entry.getKey())
                            .build());
            config.setConfigValue(entry.getValue());
            config.setUpdatedAt(LocalDateTime.now());
            salaryConfigRepository.save(config);
        }
    }

    @Override
    public int getSalaryPoolTotal() {
        return getIntConfig("salary_pool_total", DEFAULT_SALARY_POOL_TOTAL);
    }

    @Override
    public int getFormalMemberCount() {
        return getIntConfig("formal_member_count", DEFAULT_FORMAL_MEMBER_COUNT);
    }

    @Override
    public int[] getMiniCoinsRange() {
        int min = getIntConfig("mini_coins_min", DEFAULT_MINI_COINS_MIN);
        int max = getIntConfig("mini_coins_max", DEFAULT_MINI_COINS_MAX);
        return new int[]{min, max};
    }

    @Override
    public int getPointsToCoinsRatio() {
        return getIntConfig("points_to_coins_ratio", DEFAULT_POINTS_TO_COINS_RATIO);
    }

    @Override
    public List<CheckinTier> getCheckinTiers() {
        String json = getConfigValue("checkin_tiers", DEFAULT_CHECKIN_TIERS_JSON);
        try {
            return objectMapper.readValue(json, new TypeReference<List<CheckinTier>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("签到奖惩表配置 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public RotationThresholds getRotationThresholds() {
        return RotationThresholds.builder()
                .promotionPointsThreshold(getIntConfig("promotion_points_threshold", DEFAULT_PROMOTION_POINTS_THRESHOLD))
                .demotionSalaryThreshold(getIntConfig("demotion_salary_threshold", DEFAULT_DEMOTION_SALARY_THRESHOLD))
                .demotionConsecutiveMonths(getIntConfig("demotion_consecutive_months", DEFAULT_DEMOTION_CONSECUTIVE_MONTHS))
                .dismissalPointsThreshold(getIntConfig("dismissal_points_threshold", DEFAULT_DISMISSAL_POINTS_THRESHOLD))
                .dismissalConsecutiveMonths(getIntConfig("dismissal_consecutive_months", DEFAULT_DISMISSAL_CONSECUTIVE_MONTHS))
                .build();
    }

    /**
     * 配置校验逻辑：
     * 1. mini_coins_min > mini_coins_max → 拒绝
     * 2. base_allocation × formal_member_count > salary_pool_total → 拒绝
     * 3. 流转阈值为负数 → 拒绝
     */
    void validateConfig(Map<String, String> configMap) {
        // Resolve each value: use incoming configMap first, then existing DB, then default
        int miniCoinsMin = resolveIntConfig(configMap, "mini_coins_min", DEFAULT_MINI_COINS_MIN);
        int miniCoinsMax = resolveIntConfig(configMap, "mini_coins_max", DEFAULT_MINI_COINS_MAX);
        int baseAllocation = resolveIntConfig(configMap, "base_allocation", DEFAULT_BASE_ALLOCATION);
        int formalMemberCount = resolveIntConfig(configMap, "formal_member_count", DEFAULT_FORMAL_MEMBER_COUNT);
        int salaryPoolTotal = resolveIntConfig(configMap, "salary_pool_total", DEFAULT_SALARY_POOL_TOTAL);
        int promotionPointsThreshold = resolveIntConfig(configMap, "promotion_points_threshold", DEFAULT_PROMOTION_POINTS_THRESHOLD);
        int demotionSalaryThreshold = resolveIntConfig(configMap, "demotion_salary_threshold", DEFAULT_DEMOTION_SALARY_THRESHOLD);
        int dismissalPointsThreshold = resolveIntConfig(configMap, "dismissal_points_threshold", DEFAULT_DISMISSAL_POINTS_THRESHOLD);

        // Validation 1: min > max
        if (miniCoinsMin > miniCoinsMax) {
            throw new IllegalArgumentException(
                    "个人最低迷你币(" + miniCoinsMin + ")不能大于个人最高迷你币(" + miniCoinsMax + ")");
        }

        // Validation 2: allocation × count > pool
        long allocationTotal = (long) baseAllocation * formalMemberCount;
        if (allocationTotal > salaryPoolTotal) {
            throw new IllegalArgumentException(
                    "基准分配额(" + baseAllocation + ") × 正式成员数(" + formalMemberCount +
                    ") = " + allocationTotal + " 超过薪酬池总额(" + salaryPoolTotal + ")");
        }

        // Validation 3: negative rotation thresholds
        if (promotionPointsThreshold < 0) {
            throw new IllegalArgumentException(
                    "转正积分阈值不能为负数: " + promotionPointsThreshold);
        }
        if (demotionSalaryThreshold < 0) {
            throw new IllegalArgumentException(
                    "降级薪酬阈值不能为负数: " + demotionSalaryThreshold);
        }
        if (dismissalPointsThreshold < 0) {
            throw new IllegalArgumentException(
                    "开除积分阈值不能为负数: " + dismissalPointsThreshold);
        }
    }

    /**
     * Resolve a config value: incoming map → existing DB → default
     */
    private int resolveIntConfig(Map<String, String> configMap, String key, int defaultValue) {
        if (configMap.containsKey(key)) {
            try {
                return Integer.parseInt(configMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return getIntConfig(key, defaultValue);
    }
}
