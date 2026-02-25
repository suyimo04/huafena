package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 薪酬明细表 DTO
 * 包含每人详细收入和积分构成
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryReportDTO {

    /** 报告生成时间 */
    private LocalDateTime generatedAt;

    /** 薪资池总额（迷你币） */
    private int salaryPoolTotal;

    /** 实际分配总额（迷你币） */
    private int allocatedTotal;

    /** 剩余额度（迷你币） */
    private int remainingAmount;

    /** 成员明细列表 */
    @Builder.Default
    private List<MemberSalaryDetail> details = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberSalaryDetail {
        /** 用户 ID */
        private Long userId;

        /** 用户名 */
        private String username;

        /** 角色 */
        private String role;

        /** 基础积分 */
        private int basePoints;

        /** 奖励积分 */
        private int bonusPoints;

        /** 扣减 */
        private int deductions;

        /** 总积分 */
        private int totalPoints;

        /** 迷你币 */
        private int miniCoins;

        /** 薪资金额 */
        private BigDecimal salaryAmount;

        /** 备注 */
        private String remark;

        // 基础职责维度明细
        private int communityActivityPoints;
        private int checkinCount;
        private int checkinPoints;
        private int violationHandlingCount;
        private int violationHandlingPoints;
        private int taskCompletionPoints;
        private int announcementCount;
        private int announcementPoints;

        // 卓越贡献维度明细
        private int eventHostingPoints;
        private int birthdayBonusPoints;
        private int monthlyExcellentPoints;
    }
}
