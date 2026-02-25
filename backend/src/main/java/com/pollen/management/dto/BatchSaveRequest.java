package com.pollen.management.dto;

import com.pollen.management.entity.SalaryRecord;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * 薪资批量保存请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSaveRequest {

    @NotEmpty(message = "薪资记录列表不能为空")
    private List<SalaryRecord> records;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
}
