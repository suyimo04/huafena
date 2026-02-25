package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private String groupName;

    @Column(columnDefinition = "JSON")
    private String memberIds;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
