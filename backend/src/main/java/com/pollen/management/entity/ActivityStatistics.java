package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long activityId;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalRegistered = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalAttended = 0;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal checkInRate = BigDecimal.ZERO;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgFeedbackRating = BigDecimal.ZERO;

    @Column(columnDefinition = "JSON")
    private String feedbackSummary;

    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
