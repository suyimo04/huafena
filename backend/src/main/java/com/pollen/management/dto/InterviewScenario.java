package com.pollen.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试场景 DTO - 对应 interview-scenarios.json 中的场景定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewScenario {

    private String id;
    private String name;
    private String description;
    private int difficulty;
    private String category;
    @JsonProperty("isStudentOnly")
    private boolean studentOnly;
    private String aiRole;
    private String initialPrompt;
}
