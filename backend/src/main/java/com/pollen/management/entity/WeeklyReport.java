package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "new_applications", nullable = false)
    @Builder.Default
    private Integer newApplications = 0;

    @Column(name = "interviews_completed", nullable = false)
    @Builder.Default
    private Integer interviewsCompleted = 0;

    @Column(name = "new_members", nullable = false)
    @Builder.Default
    private Integer newMembers = 0;

    @Column(name = "activities_held", nullable = false)
    @Builder.Default
    private Integer activitiesHeld = 0;

    @Column(name = "total_points_issued", nullable = false)
    @Builder.Default
    private Integer totalPointsIssued = 0;

    @Column(name = "detail_data", columnDefinition = "JSON")
    private String detailData;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
