package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLogQueryRequest {

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
