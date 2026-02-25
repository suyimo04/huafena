package com.pollen.management.dto;

import com.pollen.management.entity.SalaryRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 薪资批量保存响应 DTO
 * 包含验证结果和保存后的记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSaveResponse {

    /** 是否保存成功 */
    private boolean success;

    /** 保存后的记录（仅成功时有值） */
    private List<SalaryRecord> savedRecords;

    /** 验证错误列表（失败时有值） */
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    /** 全局错误消息（如总额超限、成员数不符） */
    private String globalError;

    /** 违规记录的用户 ID 列表（用于前端高亮） */
    @Builder.Default
    private List<Long> violatingUserIds = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        /** 违规成员的用户 ID */
        private Long userId;

        /** 违规字段名 */
        private String field;

        /** 错误消息 */
        private String message;
    }
}
