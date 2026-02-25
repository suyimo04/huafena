package com.pollen.management.service;

import com.pollen.management.dto.ApplicationFormData;
import com.pollen.management.entity.Application;
import com.pollen.management.entity.ApplicationTimeline;

import java.util.List;
import java.util.Map;

/**
 * 申请服务接口 - 处理双入口（注册/公开链接）的申请创建和审核
 * V3.1: 集成自动筛选，创建申请前执行 UID/年龄/中高考筛选
 */
public interface ApplicationService {

    /**
     * 通过注册入口创建申请：关联已有用户和问卷回答，执行自动筛选。
     * 筛选拒绝→状态 AUTO_REJECTED；筛选通过→状态 PENDING_INITIAL_REVIEW。
     *
     * @param userId                  用户 ID
     * @param questionnaireResponseId 问卷回答 ID
     * @param formData                报名表单数据（含花粉UID、出生年月、学生身份、可用性承诺）
     * @return 创建的申请记录
     */
    Application createFromRegistration(Long userId, Long questionnaireResponseId, ApplicationFormData formData);

    /**
     * 通过公开链接入口创建申请：自动创建账户（APPLICANT, enabled=false），
     * 提交问卷回答，执行自动筛选，创建申请记录。
     * 筛选拒绝→状态 AUTO_REJECTED；筛选通过→状态 PENDING_INITIAL_REVIEW。
     *
     * @param linkToken 公开链接令牌
     * @param answers   问卷回答数据
     * @param formData  报名表单数据（含花粉UID、出生年月、学生身份、可用性承诺）
     * @return 创建的申请记录
     */
    Application createFromPublicLink(String linkToken, Map<String, Object> answers, ApplicationFormData formData);

    /**
     * 初审操作：通过或拒绝申请。
     * 通过：状态→INITIAL_REVIEW_PASSED，启用用户账户。
     * 拒绝：状态→REJECTED，账户保持禁用。
     *
     * @param applicationId 申请 ID
     * @param approved      是否通过
     */
    void initialReview(Long applicationId, boolean approved);

    /**
     * 查询所有申请记录，按创建时间倒序排列。
     * 包含申请者信息、问卷摘要、状态。
     *
     * @return 按创建时间倒序排列的申请列表
     */
    java.util.List<Application> listAll();

    /**
     * 批量通过初审：将所有处于 PENDING_INITIAL_REVIEW 状态的申请更新为 INITIAL_REVIEW_PASSED，
     * 并启用对应用户账户。非 PENDING_INITIAL_REVIEW 状态的申请将被跳过。
     *
     * @param applicationIds 申请 ID 列表
     */
    void batchApprove(java.util.List<Long> applicationIds);

    /**
     * 批量拒绝初审：将所有处于 PENDING_INITIAL_REVIEW 状态的申请更新为 REJECTED。
     * 非 PENDING_INITIAL_REVIEW 状态的申请将被跳过。
     *
     * @param applicationIds 申请 ID 列表
     */
    void batchReject(java.util.List<Long> applicationIds);

    /**
     * 批量发送 AI 面试通知：对所有处于 INITIAL_REVIEW_PASSED 状态的申请触发 AI 面试通知。
     * 非 INITIAL_REVIEW_PASSED 状态的申请将被跳过。
     *
     * @param applicationIds 申请 ID 列表
     */
    void batchNotifyInterview(java.util.List<Long> applicationIds);

    /**
     * 导出报名数据为 Excel 文件。
     * 根据筛选条件（状态）查询申请数据，生成包含所有申请字段的 Excel 工作簿。
     *
     * @param status 可选的状态筛选条件，为 null 时导出全部数据
     * @return Excel 文件的字节数组
     */
    byte[] exportToExcel(com.pollen.management.entity.enums.ApplicationStatus status);

    /**
     * 获取申请流程时间线，按时间正序排列。
     *
     * @param applicationId 申请 ID
     * @return 时间线节点列表
     */
    List<ApplicationTimeline> getTimeline(Long applicationId);
}

