package com.pollen.management.service;

import com.pollen.management.dto.BatchSaveResponse;
import com.pollen.management.dto.SalaryReportDTO;
import com.pollen.management.entity.SalaryRecord;

import java.util.List;

/**
 * 薪资管理服务接口
 */
public interface SalaryService {

    /**
     * 计算所有正式成员（VICE_LEADER + MEMBER）的薪资
     * 固定薪资池 2000 迷你币（对应 1000 积分）
     * 计算流程：原始积分统计 → 积分转迷你币（×2）→ 薪酬池调剂 → 绩效评议调整
     */
    List<SalaryRecord> calculateSalaries();

    /**
     * 获取当前薪资记录列表
     */
    List<SalaryRecord> getSalaryList();

    /**
     * 更新单条薪资记录（单元格编辑）
     */
    SalaryRecord updateSalaryRecord(Long id, SalaryRecord updates);

    /**
     * 批量保存薪资记录（旧接口，验证失败抛异常）
     */
    List<SalaryRecord> batchSave(List<SalaryRecord> records);

    /**
     * 批量保存薪资记录（增强版）
     * 返回结构化验证结果，包含每条违规记录的用户 ID 和错误详情
     * 验证通过时保存所有修改、生成操作日志
     * 使用乐观锁（version 字段）防止并发冲突
     *
     * @param records    待保存的薪资记录
     * @param operatorId 操作人 ID（用于审计日志）
     * @return 批量保存响应，包含成功/失败状态和详细错误信息
     */
    BatchSaveResponse batchSaveWithValidation(List<SalaryRecord> records, Long operatorId);

    /**
     * 生成薪酬明细表：每人详细收入和积分构成
     * 基于当前未归档的薪资记录生成报告
     */
    SalaryReportDTO generateSalaryReport();

    /**
     * 归档当前薪资记录
     * 将所有未归档记录标记为 archived=true，设置 archivedAt 时间
     *
     * @param operatorId 操作人 ID（用于审计日志）
     * @return 被归档的记录数量
     */
    int archiveSalaryRecords(Long operatorId);
}
