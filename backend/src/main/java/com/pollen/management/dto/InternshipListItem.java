package com.pollen.management.dto;

import com.pollen.management.entity.enums.InternshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipListItem {

    private Long id;
    private Long userId;
    private String username;
    private String mentorName;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private InternshipStatus status;
    private double taskCompletionRate;
}
