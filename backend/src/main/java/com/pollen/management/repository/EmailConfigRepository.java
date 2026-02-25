package com.pollen.management.repository;

import com.pollen.management.entity.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {
}
