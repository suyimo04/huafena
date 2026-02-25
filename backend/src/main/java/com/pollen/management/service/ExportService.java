package com.pollen.management.service;

import java.time.LocalDate;

public interface ExportService {
    byte[] exportMembers();
    byte[] exportPoints();
    byte[] exportSalary();
    byte[] exportActivities();
    byte[] exportWithDateRange(String dataType, LocalDate startDate, LocalDate endDate);
    String generateFileName(String dataType, LocalDate startDate, LocalDate endDate);
}
