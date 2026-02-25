package com.pollen.management.property;

import com.pollen.management.entity.Activity;
import com.pollen.management.entity.PointsRecord;
import com.pollen.management.entity.SalaryRecord;
import com.pollen.management.entity.User;
import com.pollen.management.entity.enums.*;
import com.pollen.management.repository.ActivityRepository;
import com.pollen.management.repository.PointsRecordRepository;
import com.pollen.management.repository.SalaryRecordRepository;
import com.pollen.management.repository.UserRepository;
import com.pollen.management.service.ExportServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for Excel export data completeness.
 *
 * Property 32: Excel 导出数据完整性
 * For any set of application data under filter conditions, the exported Excel file
 * should contain records and field values that are completely consistent with the
 * database query results.
 *
 * **Validates: Requirements 7.5, 16.3**
 */
class ExcelExportPropertyTest {

    // ========================================================================
    // Property 32: Members export — record count and field values match input
    // **Validates: Requirements 7.5, 16.3**
    // ========================================================================

    @Property(tries = 100)
    void property32_membersExportRecordCountMatchesInput(
            @ForAll("userLists") List<User> users) throws Exception {

        ExportServiceImpl service = buildService(users, List.of(), List.of(), List.of());
        byte[] excel = service.exportMembers();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            int dataRows = sheet.getLastRowNum(); // row 0 is header
            assertThat(dataRows)
                    .as("Excel data row count must equal input list size")
                    .isEqualTo(users.size());
        }
    }

    @Property(tries = 100)
    void property32_membersExportFieldValuesMatchInput(
            @ForAll("userLists") List<User> users) throws Exception {

        ExportServiceImpl service = buildService(users, List.of(), List.of(), List.of());
        byte[] excel = service.exportMembers();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                Row row = sheet.getRow(i + 1);
                assertThat((long) row.getCell(0).getNumericCellValue())
                        .isEqualTo(user.getId() != null ? user.getId() : 0);
                assertThat(row.getCell(1).getStringCellValue())
                        .isEqualTo(user.getUsername() != null ? user.getUsername() : "");
                assertThat(row.getCell(2).getStringCellValue())
                        .isEqualTo(user.getRole() != null ? user.getRole().name() : "");
                String expectedStatus = Boolean.TRUE.equals(user.getEnabled()) ? "启用" : "禁用";
                assertThat(row.getCell(3).getStringCellValue()).isEqualTo(expectedStatus);
            }
        }
    }

    // ========================================================================
    // Property 32: Points export — record count and field values match input
    // **Validates: Requirements 7.5, 16.3**
    // ========================================================================

    @Property(tries = 100)
    void property32_pointsExportRecordCountMatchesInput(
            @ForAll("pointsLists") List<PointsRecord> records) throws Exception {

        ExportServiceImpl service = buildService(List.of(), records, List.of(), List.of());
        byte[] excel = service.exportPoints();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(records.size());
        }
    }

    @Property(tries = 100)
    void property32_pointsExportFieldValuesMatchInput(
            @ForAll("pointsLists") List<PointsRecord> records) throws Exception {

        ExportServiceImpl service = buildService(List.of(), records, List.of(), List.of());
        byte[] excel = service.exportPoints();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 0; i < records.size(); i++) {
                PointsRecord rec = records.get(i);
                Row row = sheet.getRow(i + 1);
                assertThat((long) row.getCell(0).getNumericCellValue())
                        .isEqualTo(rec.getId() != null ? rec.getId() : 0);
                assertThat((long) row.getCell(1).getNumericCellValue())
                        .isEqualTo(rec.getUserId() != null ? rec.getUserId() : 0);
                assertThat(row.getCell(2).getStringCellValue())
                        .isEqualTo(rec.getPointsType() != null ? rec.getPointsType().name() : "");
                assertThat((int) row.getCell(3).getNumericCellValue())
                        .isEqualTo(rec.getAmount() != null ? rec.getAmount() : 0);
                assertThat(row.getCell(4).getStringCellValue())
                        .isEqualTo(rec.getDescription() != null ? rec.getDescription() : "");
            }
        }
    }

    // ========================================================================
    // Property 32: Salary export — record count and field values match input
    // **Validates: Requirements 7.5, 16.3**
    // ========================================================================

    @Property(tries = 100)
    void property32_salaryExportRecordCountMatchesInput(
            @ForAll("salaryLists") List<SalaryRecord> records) throws Exception {

        ExportServiceImpl service = buildService(List.of(), List.of(), records, List.of());
        byte[] excel = service.exportSalary();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(records.size());
        }
    }

    @Property(tries = 100)
    void property32_salaryExportFieldValuesMatchInput(
            @ForAll("salaryLists") List<SalaryRecord> records) throws Exception {

        ExportServiceImpl service = buildService(List.of(), List.of(), records, List.of());
        byte[] excel = service.exportSalary();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 0; i < records.size(); i++) {
                SalaryRecord rec = records.get(i);
                Row row = sheet.getRow(i + 1);
                assertThat((int) row.getCell(2).getNumericCellValue())
                        .isEqualTo(rec.getBasePoints() != null ? rec.getBasePoints() : 0);
                assertThat((int) row.getCell(3).getNumericCellValue())
                        .isEqualTo(rec.getBonusPoints() != null ? rec.getBonusPoints() : 0);
                assertThat((int) row.getCell(4).getNumericCellValue())
                        .isEqualTo(rec.getDeductions() != null ? rec.getDeductions() : 0);
                assertThat((int) row.getCell(5).getNumericCellValue())
                        .isEqualTo(rec.getTotalPoints() != null ? rec.getTotalPoints() : 0);
                assertThat((int) row.getCell(6).getNumericCellValue())
                        .isEqualTo(rec.getMiniCoins() != null ? rec.getMiniCoins() : 0);
            }
        }
    }

    // ========================================================================
    // Property 32: Activities export — record count and field values match input
    // **Validates: Requirements 7.5, 16.3**
    // ========================================================================

    @Property(tries = 100)
    void property32_activitiesExportRecordCountMatchesInput(
            @ForAll("activityLists") List<Activity> activities) throws Exception {

        ExportServiceImpl service = buildService(List.of(), List.of(), List.of(), activities);
        byte[] excel = service.exportActivities();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(activities.size());
        }
    }

    @Property(tries = 100)
    void property32_activitiesExportFieldValuesMatchInput(
            @ForAll("activityLists") List<Activity> activities) throws Exception {

        ExportServiceImpl service = buildService(List.of(), List.of(), List.of(), activities);
        byte[] excel = service.exportActivities();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 0; i < activities.size(); i++) {
                Activity act = activities.get(i);
                Row row = sheet.getRow(i + 1);
                assertThat(row.getCell(1).getStringCellValue())
                        .isEqualTo(act.getName() != null ? act.getName() : "");
                assertThat(row.getCell(3).getStringCellValue())
                        .isEqualTo(act.getActivityType() != null ? act.getActivityType().name() : "");
                assertThat(row.getCell(5).getStringCellValue())
                        .isEqualTo(act.getLocation() != null ? act.getLocation() : "");
                assertThat((int) row.getCell(6).getNumericCellValue())
                        .isEqualTo(act.getRegistrationCount() != null ? act.getRegistrationCount() : 0);
            }
        }
    }

    // ========================================================================
    // Property 32: Date-range export filters correctly
    // **Validates: Requirements 7.5, 16.3**
    // ========================================================================

    @Property(tries = 100)
    void property32_dateRangeExportFiltersCorrectly(
            @ForAll("userLists") List<User> users,
            @ForAll("dateRanges") LocalDate[] dateRange) throws Exception {

        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(java.time.LocalTime.MAX);

        long expectedCount = users.stream()
                .filter(u -> u.getCreatedAt() != null
                        && !u.getCreatedAt().isBefore(start)
                        && !u.getCreatedAt().isAfter(end))
                .count();

        ExportServiceImpl service = buildService(users, List.of(), List.of(), List.of());
        byte[] excel = service.exportWithDateRange("members", startDate, endDate);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = wb.getSheetAt(0);
            int dataRows = sheet.getLastRowNum();
            assertThat((long) dataRows)
                    .as("Date-range filtered export row count must match expected")
                    .isEqualTo(expectedCount);
        }
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<List<User>> userLists() {
        Arbitrary<User> userArb = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.of(Role.values()),
                Arbitraries.of(true, false),
                Arbitraries.of(OnlineStatus.values()),
                timestamps()
        ).as((id, username, role, enabled, status, createdAt) -> {
            User u = User.builder()
                    .id(id)
                    .username(username)
                    .password("encoded")
                    .role(role)
                    .enabled(enabled)
                    .onlineStatus(status)
                    .build();
            u.setCreatedAt(createdAt);
            return u;
        });
        return userArb.list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<PointsRecord>> pointsLists() {
        Arbitrary<PointsRecord> recArb = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.longs().between(1, 100),
                Arbitraries.of(PointsType.values()),
                Arbitraries.integers().between(-100, 500),
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(30),
                timestamps()
        ).as((id, userId, type, amount, desc, createdAt) -> {
            PointsRecord r = PointsRecord.builder()
                    .id(id).userId(userId).pointsType(type)
                    .amount(amount).description(desc).build();
            r.setCreatedAt(createdAt);
            return r;
        });
        return recArb.list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<SalaryRecord>> salaryLists() {
        Arbitrary<SalaryRecord> recArb = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.longs().between(1, 100),
                Arbitraries.integers().between(0, 500),
                Arbitraries.integers().between(0, 200),
                Arbitraries.integers().between(0, 100),
                Arbitraries.integers().between(0, 1000),
                Arbitraries.integers().between(0, 2000)
        ).as((id, userId, base, bonus, deduct, total, coins) -> {
            SalaryRecord r = SalaryRecord.builder()
                    .id(id).userId(userId)
                    .basePoints(base).bonusPoints(bonus).deductions(deduct)
                    .totalPoints(total).miniCoins(coins)
                    .salaryAmount(BigDecimal.valueOf(coins * 1.0))
                    .remark("remark")
                    .build();
            r.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
            return r;
        });
        return recArb.list().ofMinSize(0).ofMaxSize(15);
    }

    @Provide
    Arbitrary<List<Activity>> activityLists() {
        Arbitrary<Activity> actArb = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50),
                Arbitraries.of(ActivityType.values()),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.integers().between(0, 200),
                Arbitraries.of(ActivityStatus.values()),
                timestamps()
        ).as((id, name, desc, type, location, regCount, status, createdAt) -> {
            Activity a = Activity.builder()
                    .id(id).name(name).description(desc)
                    .activityType(type)
                    .activityTime(createdAt)
                    .location(location)
                    .registrationCount(regCount)
                    .status(status)
                    .createdBy(1L)
                    .build();
            a.setCreatedAt(createdAt);
            return a;
        });
        return actArb.list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<LocalDate[]> dateRanges() {
        return Arbitraries.integers().between(2020, 2030).flatMap(year ->
                Arbitraries.integers().between(1, 12).flatMap(month ->
                        Arbitraries.integers().between(1, 28).flatMap(day -> {
                            LocalDate start = LocalDate.of(year, month, day);
                            return Arbitraries.integers().between(1, 60).map(offset -> {
                                LocalDate end = start.plusDays(offset);
                                return new LocalDate[]{start, end};
                            });
                        })
                )
        );
    }

    // ========== Helpers ==========

    private Arbitrary<LocalDateTime> timestamps() {
        return Arbitraries.integers().between(2020, 2030).flatMap(year ->
                Arbitraries.integers().between(1, 12).flatMap(month ->
                        Arbitraries.integers().between(1, 28).flatMap(day ->
                                Arbitraries.integers().between(0, 23).map(hour ->
                                        LocalDateTime.of(year, month, day, hour, 0)
                                )
                        )
                )
        );
    }

    private ExportServiceImpl buildService(
            List<User> users,
            List<PointsRecord> points,
            List<SalaryRecord> salaries,
            List<Activity> activities) {

        UserRepository userRepo = mock(UserRepository.class);
        PointsRecordRepository pointsRepo = mock(PointsRecordRepository.class);
        SalaryRecordRepository salaryRepo = mock(SalaryRecordRepository.class);
        ActivityRepository activityRepo = mock(ActivityRepository.class);

        when(userRepo.findAll()).thenReturn(users);
        when(pointsRepo.findAll()).thenReturn(points);
        when(salaryRepo.findAll()).thenReturn(salaries);
        when(activityRepo.findAll()).thenReturn(activities);

        return new ExportServiceImpl(userRepo, pointsRepo, salaryRepo, activityRepo);
    }
}
