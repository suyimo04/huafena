package com.pollen.management.dto;

import com.pollen.management.entity.Interview;
import com.pollen.management.entity.InterviewMessage;
import com.pollen.management.entity.InterviewReport;
import lombok.*;

import java.util.List;

/**
 * 面试记录永久存档 DTO - 包含完整的对话内容、评分报告和复审意见
 * Validates: Requirements 5.14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewArchiveRecord {

    private Interview interview;

    private List<InterviewMessage> messages;

    private InterviewReport report;
}
