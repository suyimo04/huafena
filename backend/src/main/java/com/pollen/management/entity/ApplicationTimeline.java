package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_timeline")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 100)
    private String operator;

    @Column(length = 500)
    private String description;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
