package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * V3.1 自动筛选结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningResult {

    /** 是否通过自动筛选 */
    private boolean passed;

    /** 是否需要人工重点审核 */
    private boolean needsAttention;

    /** 拒绝原因（如有） */
    private String rejectReason;

    /** 关注标记列表 */
    private List<String> attentionFlags;
}
