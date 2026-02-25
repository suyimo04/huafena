package com.pollen.management.service;

import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.enums.PointsType;

import java.util.List;

/**
 * 积分管理服务接口
 */
public interface PointsService {

    /**
     * 增加积分：创建正数积分变动记录
     */
    PointsRecord addPoints(Long userId, PointsType pointsType, int amount, String description);

    /**
     * 扣减积分：创建负数积分变动记录
     */
    PointsRecord deductPoints(Long userId, PointsType pointsType, int amount, String description);

    /**
     * 查询用户积分记录，按时间倒序
     */
    List<PointsRecord> getPointsRecords(Long userId);

    /**
     * 统计用户总积分（所有记录之和）
     */
    int getTotalPoints(Long userId);


    /**
     * 根据月度签到次数计算签到积分
     * <20次=-20分，20-29次=-10分，30-39次=0分，40-49次=+30分，>=50次=+50分
     */
    int calculateCheckinPoints(int checkinCount);

    /**
     * 积分转迷你币换算：1 积分 = 2 迷你币
     */
    int convertPointsToMiniCoins(int points);


}
