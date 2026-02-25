package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_materials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    private String fileType;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
