package com.pollen.management.service;

import com.pollen.management.entity.QuestionnaireResponse;

import java.util.Map;

/**
 * 问卷回答服务接口
 */
public interface QuestionnaireResponseService {

    /**
     * 提交问卷回答：执行条件逻辑评估确定可见字段，验证可见字段，存储回答数据。
     *
     * @param versionId 问卷版本 ID
     * @param userId    用户 ID
     * @param answers   回答数据（fieldKey -> 回答值）
     * @return 保存后的问卷回答实体
     */
    QuestionnaireResponse submit(Long versionId, Long userId, Map<String, Object> answers);

    /**
     * 根据申请 ID 查询关联的问卷回答。
     *
     * @param applicationId 申请 ID
     * @return 问卷回答实体
     */
    QuestionnaireResponse getByApplicationId(Long applicationId);
}
