package com.pollen.management.entity;

import com.pollen.management.entity.enums.ActivityStatus;
import com.pollen.management.entity.enums.ActivityType;
import com.pollen.management.entity.enums.ApprovalMode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActivityType activityType;

    @Column(columnDefinition = "JSON")
    private String customFormFields;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ApprovalMode approvalMode = ApprovalMode.AUTO;

    @Column(nullable = false)
    private LocalDateTime activityTime;

    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Integer registrationCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.UPCOMING;

    @Column(length = 64)
    private String qrToken;

    @Column(nullable = false)
    private Long createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
