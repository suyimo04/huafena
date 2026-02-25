package com.pollen.management.service;

import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.RoleChangeHistory;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.Role;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.RoleChangeHistoryRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.util.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态成员流转服务实现
 * 实现需求 8.1：转正评议触发逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberRotationServiceImpl implements MemberRotationService {

    /** 实习成员月积分转正阈值 */
    static final int PROMOTION_POINTS_THRESHOLD = 100;

    /** 正式成员薪酬降级阈值 */
    static final int DEMOTION_SALARY_THRESHOLD = 150;

    /** 降级检测需要连续不达标的月数 */
    static final int DEMOTION_CONSECUTIVE_MONTHS = 2;

    /** 正式成员总数要求（VICE_LEADER + MEMBER） */
    static final int REQUIRED_FORMAL_MEMBER_COUNT = 5;

    /** 实习成员待开除检测需要连续不达标的月数 */
    static final int DISMISSAL_CONSECUTIVE_MONTHS = 2;

    /** 实习成员月积分待开除阈值 */
    static final int DISMISSAL_POINTS_THRESHOLD = 100;

    private final UserRepository userRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final RoleChangeHistoryRepository roleChangeHistoryRepository;

    @Override
    public List<User> checkPromotionEligibility() {
        List<User> interns = userRepository.findByRole(Role.INTERN);
        List<User> eligible = new ArrayList<>();

        for (User intern : interns) {
            if (isMonthlyPointsStable(intern.getId())) {
                eligible.add(intern);
            }
        }

        return eligible;
    }

    @Override
    public List<User> checkDemotionCandidates() {
        List<User> formalMembers = userRepository.findByRoleIn(
                List.of(Role.MEMBER, Role.VICE_LEADER));
        List<User> candidates = new ArrayList<>();

        for (User member : formalMembers) {
            if (hasSalaryBelowThresholdForConsecutiveMonths(member.getId())) {
                candidates.add(member);
            }
        }

        return candidates;
    }

    @Override
    public boolean triggerPromotionReview() {
        List<User> promotionEligible = checkPromotionEligibility();
        List<User> demotionCandidates = checkDemotionCandidates();

        if (promotionEligible.isEmpty() || demotionCandidates.isEmpty()) {
            log.info("转正评议条件不满足: 符合转正条件的实习成员 {} 人, 薪酬不达标的正式成员 {} 人",
                    promotionEligible.size(), demotionCandidates.size());
            return false;
        }

        log.info("触发转正评议流程: 符合转正条件的实习成员 {} 人, 薪酬不达标的正式成员 {} 人",
                promotionEligible.size(), demotionCandidates.size());
        return true;
    }

    @Override
    @Transactional
    public void executePromotion(Long internId, Long formalMemberId) {
        User intern = userRepository.findById(internId)
                .orElseThrow(() -> new BusinessException(404, "实习成员不存在"));
        User formalMember = userRepository.findById(formalMemberId)
                .orElseThrow(() -> new BusinessException(404, "正式成员不存在"));

        if (intern.getRole() != Role.INTERN) {
            throw new BusinessException(400, "该用户不是实习成员，无法执行转正");
        }
        if (formalMember.getRole() != Role.MEMBER && formalMember.getRole() != Role.VICE_LEADER) {
            throw new BusinessException(400, "该用户不是正式成员（MEMBER 或 VICE_LEADER），无法执行降级");
        }

        // 执行角色互换
        Role internOldRole = intern.getRole();
        Role formalMemberOldRole = formalMember.getRole();

        intern.setRole(Role.MEMBER);
        formalMember.setRole(Role.INTERN);

        userRepository.save(intern);
        userRepository.save(formalMember);

        // 记录角色变更历史
        roleChangeHistoryRepository.save(RoleChangeHistory.builder()
                .userId(intern.getId())
                .oldRole(internOldRole)
                .newRole(Role.MEMBER)
                .changedBy("system")
                .build());
        roleChangeHistoryRepository.save(RoleChangeHistory.builder()
                .userId(formalMember.getId())
                .oldRole(formalMemberOldRole)
                .newRole(Role.INTERN)
                .changedBy("system")
                .build());

        // 验证正式成员总数仍为 5
        long formalCount = userRepository.countByRole(Role.MEMBER) + userRepository.countByRole(Role.VICE_LEADER);
        if (formalCount != REQUIRED_FORMAL_MEMBER_COUNT) {
            throw new BusinessException(400,
                    "角色流转后正式成员总数异常，当前 " + formalCount + " 人，要求 " + REQUIRED_FORMAL_MEMBER_COUNT + " 人");
        }

        log.info("角色流转完成: 实习成员 {} 转正为 MEMBER, 正式成员 {} 降级为 INTERN", internId, formalMemberId);
    }

    @Override
    @Transactional
    public List<User> markForDismissal() {
        List<User> interns = userRepository.findByRole(Role.INTERN);
        List<User> marked = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);

        for (User intern : interns) {
            int lastMonthPoints = getMonthlyPoints(intern.getId(), lastMonth);
            int twoMonthsAgoPoints = getMonthlyPoints(intern.getId(), twoMonthsAgo);

            if (lastMonthPoints < DISMISSAL_POINTS_THRESHOLD && twoMonthsAgoPoints < DISMISSAL_POINTS_THRESHOLD) {
                intern.setPendingDismissal(true);
                userRepository.save(intern);
                marked.add(intern);
                log.info("实习成员 {} 连续两个月积分未达 {} 分，已标记为待开除（上月: {} 分, 前月: {} 分）",
                        intern.getId(), DISMISSAL_POINTS_THRESHOLD, lastMonthPoints, twoMonthsAgoPoints);
            }
        }

        return marked;
    }

    @Override
    public List<User> getPendingDismissalList() {
        List<User> interns = userRepository.findByRole(Role.INTERN);
        List<User> pendingDismissal = new ArrayList<>();
        for (User intern : interns) {
            if (Boolean.TRUE.equals(intern.getPendingDismissal())) {
                pendingDismissal.add(intern);
            }
        }
        return pendingDismissal;
    }

    /**
     * 检测实习成员月积分是否稳定达到阈值
     * 检查当前月的积分总和是否 >= 100
     */
    boolean isMonthlyPointsStable(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        int monthlyPoints = getMonthlyPoints(userId, currentMonth);
        return monthlyPoints >= PROMOTION_POINTS_THRESHOLD;
    }

    /**
     * 获取用户指定月份的积分总和
     */
    int getMonthlyPoints(Long userId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<PointsRecord> records = pointsRecordRepository
                .findByUserIdAndCreatedAtBetween(userId, start, end);

        return records.stream().mapToInt(PointsRecord::getAmount).sum();
    }

    /**
     * 检测正式成员是否连续两个月薪酬低于阈值
     * 通过查看最近的已归档薪资记录来判断
     */
    boolean hasSalaryBelowThresholdForConsecutiveMonths(Long userId) {
        List<SalaryRecord> archivedRecords = salaryRecordRepository
                .findByUserIdAndArchivedTrueOrderByArchivedAtDesc(userId);

        if (archivedRecords.size() < DEMOTION_CONSECUTIVE_MONTHS) {
            return false;
        }

        // 检查最近两条归档记录的总积分是否都低于阈值
        for (int i = 0; i < DEMOTION_CONSECUTIVE_MONTHS; i++) {
            SalaryRecord record = archivedRecords.get(i);
            if (record.getTotalPoints() >= DEMOTION_SALARY_THRESHOLD) {
                return false;
            }
        }

        return true;
    }
}
