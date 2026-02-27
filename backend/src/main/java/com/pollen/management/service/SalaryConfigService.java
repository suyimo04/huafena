package com.pollen.management.service;

import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.RotationThresholds;

import java.util.List;
import java.util.Map;

/**
 * 薪资配置管理服务接口
 */
public interface SalaryConfigService {

    /** 获取所有配置项 */
    Map<String, String> getAllConfig();

    /** 获取单个配置值，不存在时返回默认值 */
    String getConfigValue(String key, String defaultValue);

    /** 获取整数配置值 */
    int getIntConfig(String key, int defaultValue);

    /** 批量保存配置（含校验） */
    void saveConfig(Map<String, String> configMap);

    /** 获取薪酬池总额 */
    int getSalaryPoolTotal();

    /** 获取正式成员数量要求 */
    int getFormalMemberCount();

    /** 获取个人迷你币范围 [min, max] */
    int[] getMiniCoinsRange();

    /** 获取积分转迷你币比例 */
    int getPointsToCoinsRatio();

    /** 获取签到奖惩表配置 */
    List<CheckinTier> getCheckinTiers();

    /** 获取流转阈值配置 */
    RotationThresholds getRotationThresholds();

    /** 获取违规处理积分系数 */
    int getViolationHandlingMultiplier();

    /** 获取公告积分系数 */
    int getAnnouncementMultiplier();
}
