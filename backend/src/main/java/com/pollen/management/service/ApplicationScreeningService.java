package com.pollen.management.service;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.dto.ScreeningResult;

import java.time.LocalDate;

/**
 * V3.1 自动筛选服务接口
 * 负责花粉 UID 校验、年龄计算、中高考期判断和综合自动筛选
 */
public interface ApplicationScreeningService {

    /**
     * 综合执行自动筛选规则：
     * 自动拒绝条件（任一满足即拒绝）：
     * 1. 年龄 < 18 岁
     * 2. 中高考标识为 true 且距考试时间不足 3 个月
     * 3. 花粉 UID（QQ号）格式不合法
     *
     * 人工重点审核标记条件（通过筛选后检查）：
     * 1. 年龄 18-19 岁
     * 2. 每周可用时间 < 10 小时
     *
     * @param formData 报名表单数据
     * @return 筛选结果
     */
    ScreeningResult autoScreen(ApplicationFormData formData);

    /**
     * 花粉 UID（QQ号）格式校验：5-11位纯数字且首位不为0
     *
     * @param uid 花粉 UID
     * @return 是否合法
     */
    boolean validatePollenUid(String uid);

    /**
     * 根据出生年月自动计算年龄
     *
     * @param birthDate 出生日期
     * @return 年龄
     */
    int calculateAge(LocalDate birthDate);

    /**
     * 判断是否处于中高考备考期（距考试时间不足 3 个月）
     *
     * @param examFlag 中高考标识
     * @param examDate 考试日期
     * @return 是否处于中高考备考期
     */
    boolean isExamPeriod(boolean examFlag, LocalDate examDate);
}
