package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "internship_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internship_id", nullable = false)
    private Long internshipId;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
