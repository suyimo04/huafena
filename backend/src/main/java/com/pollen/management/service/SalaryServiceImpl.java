package com.pollen.management.service;

import com.pollen.management.dto.BatchSaveResponse;
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

    /** 固定薪资池：2000 迷你币 */
    static final int SALARY_POOL_MINI_COINS = 2000;

    /** 正式成员数量要求 */
    static final int REQUIRED_FORMAL_MEMBER_COUNT = 5;

    private final SalaryRecordRepository salaryRecordRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public List<SalaryRecord> calculateSalaries() {
        // 获取所有正式成员（VICE_LEADER + MEMBER）
        List<User> formalMembers = getFormalMembers();
        if (formalMembers.size() != REQUIRED_FORMAL_MEMBER_COUNT) {
            throw new BusinessException(400,
                    "正式成员数量不符，当前 " + formalMembers.size() + " 人，要求 " + REQUIRED_FORMAL_MEMBER_COUNT + " 人");
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
                    .build());
            allocatedTotal += record.getMiniCoins();
        }

        return SalaryReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .salaryPoolTotal(SALARY_POOL_MINI_COINS)
                .allocatedTotal(allocatedTotal)
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

    /**
     * 获取所有正式成员（VICE_LEADER + MEMBER）
     */
    List<User> getFormalMembers() {
        return userRepository.findByRoleIn(List.of(Role.VICE_LEADER, Role.MEMBER));
    }

    /**
     * 薪酬池调剂：按原始迷你币比例分配固定薪资池
     * 如果总原始迷你币为 0，则平均分配
     */
    List<Integer> adjustToPool(List<Integer> rawMiniCoinsList, int totalRawMiniCoins) {
        List<Integer> adjusted = new ArrayList<>();
        int memberCount = rawMiniCoinsList.size();

        if (totalRawMiniCoins <= 0) {
            // 所有人积分为 0 或负数时，平均分配
            int perPerson = SALARY_POOL_MINI_COINS / memberCount;
            int remainder = SALARY_POOL_MINI_COINS - perPerson * memberCount;
            for (int i = 0; i < memberCount; i++) {
                adjusted.add(perPerson + (i < remainder ? 1 : 0));
            }
        } else {
            // 按比例分配
            int allocated = 0;
            for (int i = 0; i < memberCount; i++) {
                if (i == memberCount - 1) {
                    // 最后一人获得剩余，避免舍入误差
                    adjusted.add(SALARY_POOL_MINI_COINS - allocated);
                } else {
                    int share = BigDecimal.valueOf(rawMiniCoinsList.get(i))
                            .multiply(BigDecimal.valueOf(SALARY_POOL_MINI_COINS))
                            .divide(BigDecimal.valueOf(totalRawMiniCoins), 0, RoundingMode.FLOOR)
                            .intValue();
                    adjusted.add(share);
                    allocated += share;
                }
            }
        }

        return adjusted;
    }

    /**
     * 绩效评议调整：将每人迷你币限制在 [200, 400] 范围内
     * 超出上限的部分按比例重新分配给未达上限的成员
     */
    List<Integer> performanceAdjust(List<Integer> miniCoinsList) {
        int[] coins = miniCoinsList.stream().mapToInt(Integer::intValue).toArray();
        int memberCount = coins.length;

        // 多轮调整，直到所有人都在范围内或无法再调整
        for (int iteration = 0; iteration < memberCount * 2; iteration++) {
            int surplus = 0;
            boolean allInRange = true;

            for (int coin : coins) {
                if (coin > 400) {
                    surplus += coin - 400;
                    allInRange = false;
                } else if (coin < 200) {
                    allInRange = false;
                }
            }

            if (allInRange) {
                break;
            }

            // 截断超出上限的
            for (int i = 0; i < memberCount; i++) {
                if (coins[i] > 400) {
                    coins[i] = 400;
                }
            }

            // 将多余部分分配给未达上限的成员（按比例）
            if (surplus > 0) {
                // Find members below cap that can receive surplus
                List<Integer> receiverIndices = new ArrayList<>();
                for (int i = 0; i < memberCount; i++) {
                    if (coins[i] < 400) {
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

            // 提升低于下限的成员到下限（从超出上限的成员扣除）
            for (int i = 0; i < memberCount; i++) {
                if (coins[i] < 200) {
                    coins[i] = 200;
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
        // 验证正式成员数量
        if (records.size() != REQUIRED_FORMAL_MEMBER_COUNT) {
            throw new BusinessException(400,
                    "正式成员数量不符，当前 " + records.size() + " 条记录，要求 " + REQUIRED_FORMAL_MEMBER_COUNT + " 条");
        }

        // 验证单人迷你币范围 [200, 400]
        for (SalaryRecord record : records) {
            if (record.getMiniCoins() < 200 || record.getMiniCoins() > 400) {
                throw new BusinessException(400,
                        "成员(userId=" + record.getUserId() + ")迷你币 " + record.getMiniCoins() + " 不在 [200, 400] 范围内");
            }
        }

        // 验证总额不超过 2000
        int totalMiniCoins = records.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        if (totalMiniCoins > SALARY_POOL_MINI_COINS) {
            throw new BusinessException(400,
                    "迷你币总额 " + totalMiniCoins + " 超过薪资池上限 " + SALARY_POOL_MINI_COINS);
        }
    }

    @Override
    @Transactional
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

        // 验证正式成员数量
        if (records.size() != REQUIRED_FORMAL_MEMBER_COUNT) {
            return BatchSaveResponse.builder()
                    .success(false)
                    .globalError("正式成员数量不符，当前 " + records.size() + " 条记录，要求 " + REQUIRED_FORMAL_MEMBER_COUNT + " 条")
                    .build();
        }

        // 验证单人迷你币范围 [200, 400]
        for (SalaryRecord record : records) {
            if (record.getMiniCoins() < 200 || record.getMiniCoins() > 400) {
                errors.add(BatchSaveResponse.ValidationError.builder()
                        .userId(record.getUserId())
                        .field("miniCoins")
                        .message("迷你币 " + record.getMiniCoins() + " 不在 [200, 400] 范围内")
                        .build());
                violatingUserIds.add(record.getUserId());
            }
        }

        // 验证总额不超过 2000
        int totalMiniCoins = records.stream().mapToInt(SalaryRecord::getMiniCoins).sum();
        if (totalMiniCoins > SALARY_POOL_MINI_COINS) {
            return BatchSaveResponse.builder()
                    .success(false)
                    .globalError("迷你币总额 " + totalMiniCoins + " 超过薪资池上限 " + SALARY_POOL_MINI_COINS)
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
