package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private Integer basePoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer bonusPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer deductions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer miniCoins = 0;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal salaryAmount = BigDecimal.ZERO;

    private String remark;

    @Version
    private Integer version;

    @Column(nullable = false)
    @Builder.Default
    private Boolean archived = false;

    private LocalDateTime archivedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
