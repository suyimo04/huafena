package com.pollen.management.service;

import com.pollen.management.entity.AuditLog;

import java.util.List;

/**
 * 审计日志服务接口
 */
public interface AuditLogService {

    /**
     * 记录关键操作日志
     *
     * @param operatorId    操作人ID
     * @param operationType 操作类型（如：薪资保存、角色变更、申请审核、面试复审等）
     * @param detail        操作详情
     */
    void logOperation(Long operatorId, String operationType, String detail);

    /**
     * 获取所有审计日志（按时间倒序）
     */
    List<AuditLog> getAuditLogs();

    /**
     * 按操作类型筛选审计日志（按时间倒序）
     */
    List<AuditLog> getAuditLogsByType(String operationType);
}
