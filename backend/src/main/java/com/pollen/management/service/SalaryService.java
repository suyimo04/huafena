package com.pollen.management.service;

import com.pollen.management.dto.BatchSaveResponse;
import com.pollen.management.dto.SalaryCalculationResult;
import com.pollen.management.dto.SalaryDimensionInput;
import com.pollen.management.dto.SalaryMemberDTO;
import com.pollen.management.dto.SalaryPeriodDTO;
import com.pollen.management.dto.SalaryPoolSummaryDTO;
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
     * 获取指定周期的薪资成员列表（按角色分组：LEADER, VICE_LEADER, INTERN）
     * 包含用户名、角色信息，以及已有的薪资记录数据
     *
     * @param period 薪酬周期标识，格式 "YYYY-MM"
     */
    List<SalaryMemberDTO> getSalaryMembers(String period);

    /**
     * 创建新的薪酬周期，为所有正式成员初始化空白记录
     *
     * @param period 薪酬周期标识，格式 "YYYY-MM"
     * @return 初始化的薪资记录列表
     */
    List<SalaryRecord> createPeriod(String period);

    /**
     * 获取所有薪酬周期列表（含状态信息），按周期降序排列
     */
    List<SalaryPeriodDTO> getPeriodList();

    /**
     * 获取最新的未归档周期标识
     *
     * @return 最新未归档周期，如无则返回 null
     */
    String getLatestActivePeriod();

    /**
     * 更新单条薪资记录（单元格编辑）
     */
    SalaryRecord updateSalaryRecord(Long id, SalaryRecord updates);

    /**
     * 批量保存薪资记录（旧接口，验证失败抛异常）
     */
    List<SalaryRecord> batchSave(List<SalaryRecord> records);

    /**
     * 批量保存薪资记录（增强版，按周期隔离）
     * 返回结构化验证结果，包含每条违规记录的用户 ID 和错误详情
     * 验证通过时保存所有修改、生成操作日志
     * 使用乐观锁（version 字段）防止并发冲突
     *
     * @param records    待保存的薪资记录
     * @param operatorId 操作人 ID（用于审计日志）
     * @param period     薪酬周期标识，格式 "YYYY-MM"
     * @return 批量保存响应，包含成功/失败状态和详细错误信息
     */
    BatchSaveResponse batchSaveWithValidation(List<SalaryRecord> records, Long operatorId, String period);

    /**
     * 按周期执行薪酬池分配：整合维度计算 + 薪酬池分配
     * 对指定周期的正式成员基于已保存的维度明细数据执行计算引擎，
     * 然后执行薪酬池分配（含等比例缩减和范围裁剪调剂）
     *
     * @param period 薪酬周期标识，格式 "YYYY-MM"
     * @return 分配后的薪资记录列表
     */
    List<SalaryRecord> calculateAndDistribute(String period);

    /**
     * 基于维度明细计算单个成员的积分汇总
     * 基础积分 = 社群活跃度 + 签到积分 + 违规处理积分 + 任务完成积分 + 公告积分
     * 奖励积分 = 活动举办积分 + 生日福利积分 + 月度优秀评议积分
     * 总积分 = 基础积分 + 奖励积分
     * 迷你币 = 总积分 × 换算比例
     *
     * @param input 各维度录入数据
     * @return 计算结果（含基础积分、奖励积分、总积分、迷你币等）
     * @throws IllegalArgumentException 当输入值超出允许范围时
     */
    SalaryCalculationResult calculateMemberPoints(SalaryDimensionInput input);

    /**
     * 按周期生成薪酬明细表：每人详细收入和积分构成
     *
     * @param period 薪酬周期标识，格式 "YYYY-MM"
     */
    SalaryReportDTO generateSalaryReport(String period);

    /**
     * 按周期归档薪资记录
     * 将指定周期内所有未归档记录标记为 archived=true，设置 archivedAt 时间
     *
     * @param operatorId 操作人 ID（用于审计日志）
     * @param period     薪酬周期标识，格式 "YYYY-MM"
     * @return 被归档的记录数量
     */
    int archiveSalaryRecords(Long operatorId, String period);

    /**
     * 获取指定周期的薪酬池概览（总额、已分配、剩余）
     *
     * @param period 薪酬周期标识，格式 "YYYY-MM"
     * @return 薪酬池概览数据
     */
    SalaryPoolSummaryDTO getPoolSummary(String period);
}
