package com.pollen.management.service;

import com.pollen.management.dto.InterviewArchiveRecord;
import com.pollen.management.entity.Interview;
import com.pollen.management.entity.InterviewMessage;
import com.pollen.management.entity.InterviewReport;

import java.util.List;

/**
 * AI 面试对话引擎服务接口
 */
public interface InterviewService {

    /**
     * 启动面试：创建 Interview 记录（IN_PROGRESS），发送初始 AI 消息
     */
    Interview startInterview(Long applicationId, String scenarioId);

    /**
     * 处理对话轮次：记录用户消息，生成模拟 AI 回复，记录 AI 消息
     */
    InterviewMessage processMessage(Long interviewId, String userMessage);

    /**
     * 结束面试：生成评估报告，设置面试状态为 COMPLETED，更新申请状态为 PENDING_REVIEW
     */
    InterviewReport endInterview(Long interviewId);

    /**
     * 获取面试详情（含消息列表）
     */
    Interview getInterview(Long interviewId);

    /**
     * 获取面试的所有消息
     */
    List<InterviewMessage> getMessages(Long interviewId);

    /**
     * 获取面试评估报告
     */
    InterviewReport getReport(Long interviewId);

    /**
     * 根据总分返回推荐标签
     * >= 8: "建议通过"
     * 6-7: "重点审查对话内容"
     * <= 5: "建议拒绝"
     */
    String getRecommendationLabel(int totalScore);

    /**
     * 人工复审：审核面试结果，以人工判断为准
     * 通过：更新申请状态为 INTERN_OFFERED，用户角色变更为 INTERN，启用账户
     * 拒绝：更新申请状态为 REJECTED
     */
    InterviewReport manualReview(Long interviewId, boolean approved, String reviewComment, String suggestedMentor, Long suggestedMentorId);

    /**
     * 获取完整的面试存档记录，包含对话内容、评分报告和复审意见
     * 所有面试记录永久存档，不可删除
     * Validates: Requirements 5.14
     */
    InterviewArchiveRecord getFullArchivedRecord(Long interviewId);
}
