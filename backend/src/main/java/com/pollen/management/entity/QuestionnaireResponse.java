package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaire_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long versionId;

    @Column(nullable = false)
    private Long userId;

    private Long applicationId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answers;

    @Column(updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
