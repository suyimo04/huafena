package com.pollen.management.dto;

import com.pollen.management.entity.InternshipTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipProgress {

    private double taskCompletionRate;
    private int totalPoints;
    private String mentorComment;
    private int remainingDays;
    private List<InternshipTask> tasks;
}
