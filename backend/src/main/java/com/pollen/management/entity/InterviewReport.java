package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long interviewId;

    @Column(nullable = false)
    private Integer ruleFamiliarity;

    @Column(nullable = false)
    private Integer communicationScore;

    @Column(nullable = false)
    private Integer pressureScore;

    @Column(nullable = false)
    private Integer totalScore;

    @Column(columnDefinition = "TEXT")
    private String aiComment;

    @Column(columnDefinition = "TEXT")
    private String reviewerComment;

    private String reviewResult;

    private String suggestedMentor;

    private String recommendationLabel;

    private Boolean manualApproved;

    private LocalDateTime reviewedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
