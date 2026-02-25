package com.pollen.management.service;

import com.pollen.management.entity.User;

import java.util.List;

/**
 * 动态成员流转服务接口
 * 负责检测转正评议条件和触发流转流程
 */
public interface MemberRotationService {

    /**
     * 检测符合转正条件的实习成员
     * 条件：月积分稳定达到 100 分
     *
     * @return 符合转正条件的实习成员列表
     */
    List<User> checkPromotionEligibility();

    /**
     * 检测薪酬不达标的正式成员（降级候选人）
     * 条件：连续两个月薪酬低于 150 分
     *
     * @return 薪酬不达标的正式成员列表
     */
    List<User> checkDemotionCandidates();

    /**
     * 触发转正评议流程
     * 当存在符合转正条件的实习成员且存在薪酬不达标的正式成员时触发
     *
     * @return true 如果成功触发评议流程，false 如果条件不满足
     */
    boolean triggerPromotionReview();

    /**
     * 执行角色流转（管理组评议通过后调用）
     * 实习成员→MEMBER，对应正式成员→INTERN
     * 保持正式成员总数（VICE_LEADER + MEMBER）始终为 5 人
     *
     * @param internId 待转正的实习成员ID
     * @param formalMemberId 待降级的正式成员ID
     */
    void executePromotion(Long internId, Long formalMemberId);

    /**
     * 标记待开除的实习成员
     * 连续两个月积分未达 100 分的实习成员自动标记为待开除
     *
     * @return 被标记为待开除的实习成员列表
     */
    List<User> markForDismissal();

    /**
     * 获取待开除实习成员列表
     *
     * @return 已标记为待开除的实习成员列表
     */
    List<User> getPendingDismissalList();
}
