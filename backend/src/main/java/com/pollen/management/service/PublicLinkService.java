package com.pollen.management.service;

import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.QuestionnaireVersion;

import java.util.List;
import java.util.Map;

/**
 * 公开链接服务接口
 */
public interface PublicLinkService {

    /**
     * 根据 linkToken 获取活跃的公开链接。
     * 验证链接是否存在、是否活跃、是否未过期。
     */
    PublicLink getActiveLink(String linkToken);

    /**
     * 根据 linkToken 获取关联的问卷版本配置。
     */
    QuestionnaireVersion getQuestionnaireByToken(String linkToken);

    /**
     * 通过公开链接提交问卷回答。
     * 自动创建用户账户（APPLICANT, enabled=false）并创建申请记录。
     */
    Map<String, Object> submitByToken(String linkToken, Map<String, Object> answers);

    /**
     * 生成公开问卷链接：创建包含唯一 UUID token 的公开链接，关联指定模板的活跃版本。
     *
     * @param templateId 问卷模板 ID
     * @param createdBy  创建者用户 ID
     * @return 创建的公开链接
     */
    PublicLink generate(Long templateId, Long createdBy);

    /**
     * 查询所有公开链接列表。
     *
     * @return 所有公开链接
     */
    List<PublicLink> listAll();
}
