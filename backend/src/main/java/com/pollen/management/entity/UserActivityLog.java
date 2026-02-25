package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private LocalDateTime actionTime;

    private Integer durationMinutes;

    @PrePersist
    protected void onCreate() {
        if (actionTime == null) {
            actionTime = LocalDateTime.now();
        }
    }
}
