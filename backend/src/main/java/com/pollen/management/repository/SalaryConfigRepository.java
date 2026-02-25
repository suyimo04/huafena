package com.pollen.management.repository;

import com.pollen.management.entity.SalaryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryConfigRepository extends JpaRepository<SalaryConfig, Long> {

    Optional<SalaryConfig> findByConfigKey(String configKey);
}
