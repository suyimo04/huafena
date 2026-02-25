package com.pollen.management.entity;

import com.pollen.management.entity.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"activityId", "userId"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.APPROVED;

    @Column(columnDefinition = "JSON")
    private String extraFields;

    @Column(nullable = false)
    @Builder.Default
    private Boolean checkedIn = false;

    private LocalDateTime checkedInAt;

    @Column(updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
