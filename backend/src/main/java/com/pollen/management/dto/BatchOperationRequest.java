package com.pollen.management.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchOperationRequest {

    @NotEmpty(message = "申请ID列表不能为空")
    private List<Long> applicationIds;
}
