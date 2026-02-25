package com.pollen.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInternshipTaskRequest {

    private String taskName;
    private String taskDescription;
    private LocalDate deadline;
}
