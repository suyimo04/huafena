package com.pollen.management.repository;

import com.pollen.management.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Optional<WeeklyReport> findByWeekStartAndWeekEnd(LocalDate weekStart, LocalDate weekEnd);

    List<WeeklyReport> findAllByOrderByWeekStartDesc();
}
