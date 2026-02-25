package com.pollen.management.service;

import com.pollen.management.dto.InterviewScenario;

import java.util.List;

/**
 * 面试场景加载服务接口 - 从本地 JSON 文件加载面试场景
 */
public interface InterviewScenarioService {

    /**
     * 获取所有可用的面试场景。
     * 当 isStudent 为 true 时，包含学生专属场景（学业与工作平衡）。
     * 当 isStudent 为 false 时，排除学生专属场景。
     *
     * @param isStudent 是否为学生身份
     * @return 面试场景列表
     */
    List<InterviewScenario> getScenarios(boolean isStudent);

    /**
     * 根据难度级别筛选面试场景。
     * 当 isStudent 为 true 时，包含学生专属场景。
     *
     * @param difficulty 难度级别（1=初级, 2=标准, 3=挑战）
     * @param isStudent  是否为学生身份
     * @return 符合难度级别的面试场景列表
     */
    List<InterviewScenario> getScenariosByDifficulty(int difficulty, boolean isStudent);
}
