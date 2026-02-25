package com.pollen.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
