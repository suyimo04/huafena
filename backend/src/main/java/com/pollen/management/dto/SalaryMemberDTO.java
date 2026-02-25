package com.pollen.management.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * 薪资管理页面成员数据 DTO，包含用户信息和薪资记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryMemberDTO {
    private Long id;          // salary record id (null if no record yet)
    private Long userId;
    private String username;
    private String role;
    private Integer basePoints;
    private Integer bonusPoints;
    private Integer deductions;
    private Integer totalPoints;
    private Integer miniCoins;
    private BigDecimal salaryAmount;
    private String remark;
    private Integer version;

    // 基础职责维度明细
    private Integer communityActivityPoints;
    private Integer checkinCount;
    private Integer checkinPoints;
    private Integer violationHandlingCount;
    private Integer violationHandlingPoints;
    private Integer taskCompletionPoints;
    private Integer announcementCount;
    private Integer announcementPoints;

    // 卓越贡献维度明细
    private Integer eventHostingPoints;
    private Integer birthdayBonusPoints;
    private Integer monthlyExcellentPoints;
}
