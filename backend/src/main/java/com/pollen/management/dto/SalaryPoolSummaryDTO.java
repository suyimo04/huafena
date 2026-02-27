package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPoolSummaryDTO {
    /** 薪酬池总额 */
    private int total;
    /** 已分配（正式成员 miniCoins 之和） */
    private int allocated;
    /** 剩余 = total - allocated */
    private int remaining;
}
