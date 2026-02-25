package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员薪酬排行项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSalaryRank {

    private Long userId;
    private String username;
    private int totalPoints;
    private int miniCoins;
}
