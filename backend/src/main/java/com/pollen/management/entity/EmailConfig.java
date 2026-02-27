package com.pollen.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "smtp_host", nullable = false, length = 200)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "smtp_username", nullable = false, length = 200)
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted", nullable = false, length = 500)
    private String smtpPasswordEncrypted;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "ssl_enabled")
    @Builder.Default
    private Boolean sslEnabled = true;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
