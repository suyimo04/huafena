package com.pollen.management.repository;

import com.pollen.management.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Page<EmailLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<EmailLog> findByRecipientAndCreatedAtAfter(String recipient, LocalDateTime after);
}
