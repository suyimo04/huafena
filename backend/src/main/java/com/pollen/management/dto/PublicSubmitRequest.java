package com.pollen.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicSubmitRequest {

    @NotNull(message = "回答数据不能为空")
    private Map<String, Object> answers;
}
