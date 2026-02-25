package com.pollen.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 薪资归档请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchiveRequest {

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
}
