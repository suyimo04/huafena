package com.pollen.management.service;

import com.pollen.management.config.RedisConfig;
import com.pollen.management.dto.BatchSaveResponse;
import com.pollen.management.dto.CheckinTier;
import com.pollen.management.dto.SalaryCalculationResult;
import com.pollen.management.dto.SalaryDimensionInput;
import com.pollen.management.dto.SalaryMemberDTO;
import com.pollen.management.dto.SalaryReportDTO;
import com.pollen.management.entity.AuditLog;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.AuditLogRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private final SalaryRecordRepository salaryRecordRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final AuditLogRepository auditLogRepository;
    private final SalaryConfigService salaryConfigService;

    @Override
    @Transactional
    public List<SalaryRecord> calculateSalaries() {
        // 获取所有正式成员（VICE_LEADER + MEMBER）
        List<User> formalMembers = getFormalMembers();
        int requiredCount = salaryConfigService.getFormalMemberCount();
        if (formalMembers.size() != requiredCount) {
            throw new BusinessException(400,
                    "正式成员数量不符，当前 " + formalMembers.size() + " 人，要求 " + requiredCount + " 人");
        }

        List<SalaryRecord> records = new ArrayList<>();

        // Step 1: 原始积分统计 & Step 2: 积分转迷你币（×2）
        int totalRawMiniCoins = 0;
        List<Integer> rawMiniCoinsList = new ArrayList<>();

        for (User member : formalMembers) {
            int totalPoints = pointsService.getTotalPoints(member.getId());
            int miniCoins = pointsService.convertPointsToMiniCoins(totalPoints);
            rawMiniCoinsList.add(miniCoins);
            totalRawMiniCoins += miniCoins;
        }

        // Step 3: 薪酬池调剂 — 按比例分配薪资池
        List<Integer> adjustedMiniCoins = adjustToPool(rawMiniCoinsList, totalRawMiniCoins);

        // Step 4: 绩效评议调整 — 确保每人在 [200, 400] 范围内
        List<Integer> finalMiniCoins = performanceAdjust(adjustedMiniCoins);

        // 构建薪资记录
        for (int i = 0; i < formalMembers.size(); i++) {
            User member = formalMembers.get(i);
            int totalPoints = pointsService.getTotalPoints(member.getId());
            int coins = finalMiniCoins.get(i);

            SalaryRecord record = SalaryRecord.builder()
                    .userId(member.getId())
                    .basePoints(totalPoints)
                    .bonusPoints(0)
                    .deductions(0)
                    .totalPoints(totalPoints)
                    .miniCoins(coins)
                    .salaryAmount(new BigDecimal(coins))
                    .remark("系统自动计算")
                    .build();

            records.add(salaryRecordRepository.save(record));
        }

        return records;
    }

    @Override
    public List<SalaryRecord> getSalaryList() {
        return salaryRecordRepository.findAll();
    }

    @Override
    public List<SalaryMemberDTO> getSalaryMembers() {
        // 获取 LEADER, VICE_LEADER, INTERN 角色的成员
        List<User> members = userRepository.findByRoleIn(
                List.of(Role.LEADER, Role.VICE_LEADER, Role.INTERN));

        // 获取所有未归档的薪资记录，按 userId 索引
        List<SalaryRecord> allRecords = salaryRecordRepository.findAll();
        var recordMap = allRecords.stream()
                .filter(r -> !r.getArchived())
                .collect(Collectors.toMap(SalaryRecord::getUserId, r -> r, (a, b) -> b));

        // 按角色排序：LEADER → VICE_LEADER → INTERN
        List<User> sorted = members.stream()
                .sorted((a, b) -> roleOrder(a.getRole()) - roleOrder(b.getRole()))
                .collect(Collectors.toList());

        List<SalaryMemberDTO> result = new ArrayList<>();
        for (User user : sorted) {
            SalaryRecord record = recordMap.get(user.getId());
            result.add(SalaryMemberDTO.builder()
                    .id(record != null ? record.getId() : null)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .basePoints(record != null ? record.getBasePoints() : 0)
                    .bonusPoints(record != null ? record.getBonusPoints() : 0)
                    .deductions(record != null ? record.getDeductions() : 0)
                    .totalPoints(record != null ? record.getTotalPoints() : 0)
                    .miniCoins(record != null ? record.getMiniCoins() : 0)
                    .salaryAmount(record != null ? record.getSalaryAmount() : BigDecimal.ZERO)
                    .remark(record != null ? record.getRemark() : null)
                    .version(record != null ? record.getVersion() : null)
                    // 基础职责维度明细
                    .communityActivityPoints(record != null ? record.getCommunityActivityPoints() : 0)
                    .checkinCount(record != null ? record.getCheckinCount() : 0)
                    .checkinPoints(record != null ? record.getCheckinPoints() : 0)
                    .violationHandlingCount(record != null ? record.getViolationHandlingCount() : 0)
                    .violationHandlingPoints(record != null ? record.getViolationHandlingPoints() : 0)
                    .taskCompletionPoints(record != null ? record.getTaskCompletionPoints() : 0)
                    .announcementCount(record != null ? record.getAnnouncementCount() : 0)
                    .announcementPoints(record != null ? record.getAnnouncementPoints() : 0)
                    // 卓越贡献维度明细
                    .eventHostingPoints(record != null ? record.getEventHostingPoints() : 0)
                    .birthdayBonusPoints(record != null ? record.getBirthdayBonusPoints() : 0)
                    .monthlyExcellentPoints(record != null ? record.getMonthlyExcellentPoints() : 0)
                    .build());
        }
        return result;
    }

    private int roleOrder(Role role) {
        return switch (role) {
            case LEADER -> 0;
            case VICE_LEADER -> 1;
            case INTERN -> 2;
            default -> 3;
        };
    }

    @Override
    @Transactional
    public SalaryRecord updateSalaryRecord(Long id, SalaryRecord updates) {
        SalaryRecord existing = salaryRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "薪资记录不存在"));

        if (updates.getBasePoints() != null) {
            existing.setBasePoints(updates.getBasePoints());
        }
        if (updates.getBonusPoints() != null) {
            existing.setBonusPoints(updates.getBonusPoints());
        }
        if (updates.getDeductions() != null) {
            existing.setDeductions(updates.getDeductions());
        }
        if (updates.getTotalPoints() != null) {
            existing.setTotalPoints(updates.getTotalPoints());
        }
        if (updates.getMiniCoins() != null) {
            existing.setMiniCoins(updates.getMiniCoins());
        }
        if (updates.getSalaryAmount() != null) {
            existing.setSalaryAmount(updates.getSalaryAmount());
        }
        if (updates.getRemark() != null) {
            existing.setRemark(updates.getRemark());
        }

        return salaryRecordRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_DASHBOARD, allEntries = true)
    public List<SalaryRecord> batchSave(List<SalaryRecord> records) {
        validateBatch(records);
        return salaryRecordRepository.saveAll(records);
    }

    @Override
    public SalaryReportDTO generateSalaryReport() {
        List<SalaryRecord> currentRecords = salaryRecordRepository.findByArchivedFalse();
        if (currentRecords.isEmpty()) {
            throw new BusinessException(404, "当前没有未归档的薪资记录");
        }

        List<SalaryReportDTO.MemberSalaryDetail> details = new ArrayList<>();
        int allocatedTotal = 0;

        for (SalaryRecord record : currentRecords) {
            String username = "unknown";
            String role = "unknown";
            User user = userRepository.findById(record.getUserId()).orElse(null);
            if (user != null) {
                username = user.getUsername();
                role = user.getRole().name();
            }

            details.add(SalaryReportDTO.MemberSalaryDetail.builder()
                    .userId(record.getUserId())
                    .username(username)
                    .role(role)
                    .basePoints(record.getBasePoints())
                    .bonusPoints(record.getBonusPoints())
                    .deductions(record.getDeductions())
                    .totalPoints(record.getTotalPoints())
                    .miniCoins(record.getMiniCoins())
                    .salaryAmount(record.getSalaryAmount())
                    .remark(record.getRemark())
                    // 基础职责维度明细
                    .communityActivityPoints(record.getCommunityActivityPoints())
                    .checkinCount(record.getCheckinCount())
                    .checkinPoints(record.getCheckinPoints())
                    .violationHandlingCount(record.getViolationHandlingCount())
                    .violationHandlingPoints(record.getViolationHandlingPoints())
                    .taskCompletionPoints(record.getTaskCompletionPoints())
                    .announcementCount(record.getAnnouncementCount())
                    .announcementPoints(record.getAnnouncementPoints())
                    // 卓越贡献维度明细
                    .eventHostingPoints(record.getEventHostingPoints())
                    .birthdayBonusPoints(record.getBirthdayBonusPoints())
                    .monthlyExcellentPoints(record.getMonthlyExcellentPoints())
                    .build());
            allocatedTotal += record.getMiniCoins();
        }

        int salaryPoolTotal = salaryConfigService.getSalaryPoolTotal();

        return SalaryReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .salaryPoolTotal(salaryPoolTotal)
                .allocatedTotal(allocatedTotal)
                .remainingAmount(salaryPoolTotal - allocatedTotal)
                .details(details)
                .build();
    }

    @Override
    @Transactional
    public int archiveSalaryRecords(Long operatorId) {
        List<SalaryRecord> currentRecords = salaryRecordRepository.findByArchivedFalse();
        if (currentRecords.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        for (SalaryRecord record : currentRecords) {
            record.setArchived(true);
            record.setArchivedAt(now);
        }
        salaryRecordRepository.saveAll(currentRecords);

        // 生成审计日志
        String detail = "归档薪资记录 " + currentRecords.size() + " 条，用户ID: " +
                currentRecords.stream().map(r -> String.valueOf(r.getUserId()))
                        .collect(Collectors.joining(", "));

        AuditLog log = AuditLog.builder()
                .operatorId(operatorId)
                .operationType("SALARY_ARCHIVE")
                .operationTime(now)
                .operationDetail(detail)
                .build();
        auditLogRepository.save(log);

        return currentRecords.size();
    }

    @Override
    public SalaryCalculationResult calculateMemberPoints(SalaryDimensionInput input) {
        validateDimensionInput(input);

        List<CheckinTier> tiers = salaryConfigService.getCheckinTiers();
        int pointsToCoinsRatio = salaryConfigService.getPointsToCoinsRatio();

        int checkinPoints = lookupCheckinTier(input.getCheckinCount(), tiers);
        String checkinLevel = lookupCheckinLevel(input.getCheckinCount(), tiers);
        int violationHandlingPoints = input.getViolationHandlingCount() * 3;
        int announcementPoints = input.getAnnouncementCount() * 5;

        int basePoints = input.getCommunityActivityPoints()
                + checkinPoints
                + violationHandlingPoints
                + input.getTaskCompletionPoints()
                + announcementPoints;

        int bonusPoints = input.getEventHostingPoints()
                + input.getBirthdayBonusPoints()
                + input.getMonthlyExcellentPoints();

        int totalPoints = basePoints + bonusPoints;
        int miniCoins = totalPoints * pointsToCoinsRatio;

        return SalaryCalculationResult.builder()
                .basePoints(basePoints)
                .bonusPoints(bonusPoints)
                .totalPoints(totalPoints)
                .miniCoins(miniCoins)
                .checkinPoints(checkinPoints)
                .violationHandlingPoints(violationHandlingPoints)
                .announcementPoints(announcementPoints)
                .checkinLevel(checkinLevel)
                .build();
    }

    @Override
    @Transactional
    public List<SalaryRecord> calculateAndDistribute() {
        // 获取所有未归档的薪资记录
        List<SalaryRecord> currentRecords = salaryRecordRepository.findByArchivedFalse();
        if (currentRecords.isEmpty()) {
            throw new BusinessException(404, "当前没有未归档的薪资记录，请先录入数据");
        }

        List<CheckinTier> tiers = salaryConfigService.getCheckinTiers();
        int pointsToCoinsRatio = salaryConfigService.getPointsToCoinsRatio();

        // Step 1: 对每条记录基于维度明细重新计算积分
        List<Integer> rawMiniCoinsList = new ArrayList<>();
        int totalRawMiniCoins = 0;

        for (SalaryRecord record : currentRecords) {
            // 计算签到积分
            int checkinPoints = lookupCheckinTier(record.getCheckinCount(), tiers);
            record.setCheckinPoints(checkinPoints);

            // 计算违规处理积分
            int violationHandlingPoints = record.getViolationHandlingCount() * 3;
            record.setViolationHandlingPoints(violationHandlingPoints);

            // 计算公告积分
            int announcementPoints = record.getAnnouncementCount() * 5;
            record.setAnnouncementPoints(announcementPoints);

            // 基础积分汇总
            int basePoints = record.getCommunityActivityPoints()
                    + checkinPoints
                    + violationHandlingPoints
                    + record.getTaskCompletionPoints()
                    + announcementPoints;
            record.setBasePoints(basePoints);

            // 奖励积分汇总
            int bonusPoints = record.getEventHostingPoints()
                    + record.getBirthdayBonusPoints()
                    + record.getMonthlyExcellentPoints();
            record.setBonusPoints(bonusPoints);

            // 总积分
            int totalPoints = basePoints + bonusPoints;
            record.setTotalPoints(totalPoints);

            // 原始迷你币
            int miniCoins = totalPoints * pointsToCoinsRatio;
            rawMiniCoinsList.add(miniCoins);
            totalRawMiniCoins += miniCoins;
        }

        // Step 2: 薪酬池分配（等比例缩减）
        List<Integer> adjustedMiniCoins = adjustToPool(rawMiniCoinsList, totalRawMiniCoins);

        // Step 3: 绩效评议调整（范围裁剪与调剂）
        List<Integer> finalMiniCoins = performanceAdjust(adjustedMiniCoins);

        // Step 4: 更新记录
        for (int i = 0; i < currentRecords.size(); i++) {
            SalaryRecord record = currentRecords.get(i);
            record.setMiniCoins(finalMiniCoins.get(i));
            record.setSalaryAmount(new BigDecimal(finalMiniCoins.get(i)));
        }

        return salaryRecordRepository.saveAll(currentRecords);
    }

    /**
     * 签到奖惩分级查表：根据签到次数查找对应积分
     * 负数签到次数视为 0 次处理
     */
    int lookupCheckinTier(int count, List<CheckinTier> tiers) {
        if (count < 0) {
            count = 0;
        }
        for (CheckinTier tier : tiers) {
            if (count >= tier.getMinCount() && count <= tier.getMaxCount()) {
                return tier.getPoints();
            }
        }
        return 0;
    }

    /**
     * 签到奖惩分级查表：根据签到次数查找对应等级标记
     * 负数签到次数视为 0 次处理
     */
    String lookupCheckinLevel(int count, List<CheckinTier> tiers) {
        if (count < 0) {
            count = 0;
        }
        for (CheckinTier tier : tiers) {
            if (count >= tier.getMinCount() && count <= tier.getMaxCount()) {
                return tier.getLabel();
            }
        }
        return null;
    }

    /**
     * 积分维度输入范围校验
     * 超出范围时抛出 IllegalArgumentException
     */
    void validateDimensionInput(SalaryDimensionInput input) {
        if (input.getCommunityActivityPoints() < 0 || input.getCommunityActivityPoints() > 100) {
            throw new IllegalArgumentException(
                    "社群活跃度积分超出范围，合法范围: 0-100，当前值: " + input.getCommunityActivityPoints());
        }
        if (input.getTaskCompletionPoints() < 0 || input.getTaskCompletionPoints() > 100) {
            throw new IllegalArgumentException(
                    "任务完成积分超出范围，合法范围: 0-100，当前值: " + input.getTaskCompletionPoints());
        }
        if (input.getViolationHandlingCount() < 0) {
            throw new IllegalArgumentException(
                    "违规处理次数不能为负数，当前值: " + input.getViolationHandlingCount());
        }
        if (input.getAnnouncementCount() < 0) {
            throw new IllegalArgumentException(
                    "公告发布次数不能为负数，当前值: " + input.getAnnouncementCount());
        }
        if (input.getEventHostingPoints() < 0 || input.getEventHostingPoints() > 250) {
            throw new IllegalArgumentException(
                    "活动举办积分超出范围，合法范围: 0-250，当前值: " + input.getEventHostingPoints());
        }
        if (input.getBirthdayBonusPoints() < 0 || input.getBirthdayBonusPoints() > 25) {
            throw new IllegalArgumentException(
                    "生日福利积分超出范围，合法范围: 0-25，当前值: " + input.getBirthdayBonusPoints());
        }
        if (input.getMonthlyExcellentPoints() < 0 || input.getMonthlyExcellentPoints() > 30) {
            throw new IllegalArgumentException(
                    "月度优秀评议积分超出范围，合法范围: 0-30，当前值: " + input.getMonthlyExcellentPoints());
        }
    }

    /**
     * 获取所有正式成员（VICE_LEADER + MEMBER）
     */
    List<User> getFormalMembers() {
        return userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER));
    }

    /**
     * 薪酬池分配：等比例缩减逻辑
     * - 总原始迷你币 <= 0 时，平均分配薪酬池
     * - 总原始迷你币 > 薪酬池时，按比例缩减至薪酬池总额（需求 4.2）
     * - 总原始迷你币 <= 薪酬池时，保留原始值不变（需求 4.3）
     */
    List<Integer> adjustToPool(List<Integer> rawMiniCoinsList, int totalRawMiniCoins) {
        int salaryPoolTotal = salaryConfigService.getSalaryPoolTotal();
        int memberCount = rawMiniCoinsList.size();

        if (totalRawMiniCoins <= 0) {
            // 所有人积分为 0 或负数时，平均分配
            List<Integer> adjusted = new ArrayList<>();
            int perPerson = salaryPoolTotal / memberCount;
            int remainder = salaryPoolTotal - perPerson * memberCount;
            for (int i = 0; i < memberCount; i++) {
                adjusted.add(perPerson + (i < remainder ? 1 : 0));
            }
            return adjusted;
        }

        if (totalRawMiniCoins <= salaryPoolTotal) {
            // 总和未超过薪酬池，保留原始值不变（需求 4.3）
            return new ArrayList<>(rawMiniCoinsList);
        }

        // 总和超过薪酬池，等比例缩减（需求 4.2）
        List<Integer> adjusted = new ArrayList<>();
        int allocated = 0;
        for (int i = 0; i < memberCount; i++) {
            if (i == memberCount - 1) {
                // 最后一人获得剩余，避免舍入误差
                adjusted.add(salaryPoolTotal - allocated);
            } else {
                int share = BigDecimal.valueOf(rawMiniCoinsList.get(i))
                        .multiply(BigDecimal.valueOf(salaryPoolTotal))
                        .divide(BigDecimal.valueOf(totalRawMiniCoins), 0, RoundingMode.FLOOR)
                        .intValue();
                adjusted.add(share);
                allocated += share;
            }
        }

        return adjusted;
    }

    /**
     * 绩效评议调整：范围裁剪与调剂逻辑
     * - 每位成员的最终迷你币在配置的 [min, max] 范围内（需求 4.4）
     * - 超出 max 的部分调剂给未达 max 的成员（需求 4.5）
     * - 总额不超过薪酬池总额（需求 4.6）
     */
    List<Integer> performanceAdjust(List<Integer> miniCoinsList) {
        int[] range = salaryConfigService.getMiniCoinsRange();
        int minCoins = range[0];
        int maxCoins = range[1];
        int[] coins = miniCoinsList.stream().mapToInt(Integer::intValue).toArray();
        int memberCount = coins.length;

        // 多轮调整，直到所有人都在范围内或无法再调整
        for (int iteration = 0; iteration < memberCount * 2; iteration++) {
            int surplus = 0;
            boolean allInRange = true;

            for (int coin : coins) {
                if (coin > maxCoins) {
                    surplus += coin - maxCoins;
                    allInRange = false;
                } else if (coin < minCoins) {
                    allInRange = false;
                }
            }

            if (allInRange) {
                break;
            }

            // 截断超出上限的
            for (int i = 0; i < memberCount; i++) {
                if (coins[i] > maxCoins) {
                    coins[i] = maxCoins;
                }
            }

            // 将多余部分分配给未达上限的成员（需求 4.5）
            if (surplus > 0) {
                List<Integer> receiverIndices = new ArrayList<>();
                for (int i = 0; i < memberCount; i++) {
                    if (coins[i] < maxCoins) {
                        receiverIndices.add(i);
                    }
                }
                if (receiverIndices.isEmpty()) {
                    break; // All at cap, can't redistribute
                }
                int perReceiver = surplus / receiverIndices.size();
                int remainder = surplus % receiverIndices.size();
                for (int idx = 0; idx < receiverIndices.size(); idx++) {
                    int i = receiverIndices.get(idx);
                    coins[i] += perReceiver + (idx < remainder ? 1 : 0);
                }
            }

            // 提升低于下限的成员到下限
            for (int i = 0; i < memberCount; i++) {
                if (coins[i] < minCoins) {
                    coins[i] = minCoins;
                }
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int coin : coins) {
            result.add(coin);
        }
        return result;
    }

    /**
     * 批量保存验证
     */
    void validateBatch(List<SalaryRecord> records) {
        int requiredCount = salaryConfigService.getFormalMemberCount();
        int[] range = salaryConfigService.getMiniCoinsRange();
        int minCoins = range[0];
        int maxCoins = range[1];
        int salaryPoolTotal = salaryConfigService.getSalaryPoolTotal();

        // 验证正式成员数量
        if (records.size() != requiredCount) {
            throw new BusinessException(400,
                    "正式成员数量不符，当前 " + records.size() + " 条记录，要求 " + requiredCount + " 条");
        }

        // 验证单人迷你币范围
        for (SalaryRecord record : records) {
            if (record.getMiniCoins() < minCoins || record.getMiniCoins() > maxCoins) {
                throw new BusinessException(400,
                        "成员(userId=" + record.getUserId() + ")迷你币 " + record.getMiniCoins() + " 不在 [" + minCoins + ", " + maxCoins + "] 范围内");
            }
        }

        // 验证总额不超过薪酬池
        int totalMiniCoins = records.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        if (totalMiniCoins > salaryPoolTotal) {
            throw new BusinessException(400,
                    "迷你币总额 " + totalMiniCoins + " 超过薪资池上限 " + salaryPoolTotal);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_DASHBOARD, allEntries = true)
    public BatchSaveResponse batchSaveWithValidation(List<SalaryRecord> records, Long operatorId) {
        // Step 1: 结构化验证
        BatchSaveResponse validationResult = validateBatchDetailed(records);
        if (!validationResult.isSuccess()) {
            return validationResult;
        }

        // Step 2: 保存所有记录（乐观锁由 @Version 自动处理）
        try {
            List<SalaryRecord> saved = salaryRecordRepository.saveAll(records);

            // Step 3: 生成操作日志
            String detail = "批量保存薪资记录 " + saved.size() + " 条，用户ID: " +
                    saved.stream().map(r -> String.valueOf(r.getUserId()))
                            .collect(Collectors.joining(", "));

            AuditLog log = AuditLog.builder()
                    .operatorId(operatorId)
                    .operationType("SALARY_BATCH_SAVE")
                    .operationTime(LocalDateTime.now())
                    .operationDetail(detail)
                    .build();
            auditLogRepository.save(log);

            return BatchSaveResponse.builder()
                    .success(true)
                    .savedRecords(saved)
                    .build();
        } catch (ObjectOptimisticLockingFailureException e) {
            return BatchSaveResponse.builder()
                    .success(false)
                    .globalError("并发修改冲突，请刷新后重试")
                    .build();
        }
    }

    /**
     * 结构化批量验证，返回所有违规记录的详细错误信息
     */
    BatchSaveResponse validateBatchDetailed(List<SalaryRecord> records) {
        List<BatchSaveResponse.ValidationError> errors = new ArrayList<>();
        List<Long> violatingUserIds = new ArrayList<>();
        int requiredCount = salaryConfigService.getFormalMemberCount();
        int[] range = salaryConfigService.getMiniCoinsRange();
        int minCoins = range[0];
        int maxCoins = range[1];
        int salaryPoolTotal = salaryConfigService.getSalaryPoolTotal();

        // 验证正式成员数量
        if (records.size() != requiredCount) {
            return BatchSaveResponse.builder()
                    .success(false)
                    .globalError("正式成员数量不符，当前 " + records.size() + " 条记录，要求 " + requiredCount + " 条")
                    .build();
        }

        // 验证单人迷你币范围
        for (SalaryRecord record : records) {
            if (record.getMiniCoins() < minCoins || record.getMiniCoins() > maxCoins) {
                errors.add(BatchSaveResponse.ValidationError.builder()
                        .userId(record.getUserId())
                        .field("miniCoins")
                        .message("迷你币 " + record.getMiniCoins() + " 不在 [" + minCoins + ", " + maxCoins + "] 范围内")
                        .build());
                violatingUserIds.add(record.getUserId());
            }
        }

        // 验证总额不超过薪酬池
        int totalMiniCoins = records.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        if (totalMiniCoins > salaryPoolTotal) {
            return BatchSaveResponse.builder()
                    .success(false)
                    .globalError("迷你币总额 " + totalMiniCoins + " 超过薪资池上限 " + salaryPoolTotal)
                    .errors(errors)
                    .violatingUserIds(violatingUserIds)
                    .build();
        }

        if (!errors.isEmpty()) {
            return BatchSaveResponse.builder()
                    .success(false)
                    .errors(errors)
                    .violatingUserIds(violatingUserIds)
                    .build();
        }

        return BatchSaveResponse.builder().success(true).build();
    }
}
