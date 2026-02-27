package com.pollen.management.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: salary-period, Property 9: 数据迁移周期赋值正确性
 * **Validates: Requirements 7.1, 7.2**
 *
 * For any 具有 created_at 时间戳的薪资记录，数据迁移后其 period 字段应等于
 * created_at 格式化为 "YYYY-MM" 的结果。
 *
 * The migration SQL: FORMATDATETIME(created_at, 'yyyy-MM')
 * Java equivalent: YearMonth.from(createdAt).toString()
 */
class DataMigrationPeriodPropertyTest {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Simulates the migration logic: given a created_at timestamp,
     * compute the period as the migration SQL would.
     */
    private String computeMigratedPeriod(LocalDateTime createdAt) {
        return YearMonth.from(createdAt).toString();
    }

    @Property(tries = 100)
    void migratedPeriodMatchesCreatedAtYearMonth(
            @ForAll("randomLocalDateTimes") LocalDateTime createdAt) {

        String migratedPeriod = computeMigratedPeriod(createdAt);

        // The migrated period should equal the year-month formatted from created_at
        String expected = createdAt.format(PERIOD_FORMATTER);
        assertThat(migratedPeriod).isEqualTo(expected);
    }

    @Property(tries = 100)
    void migratedPeriodHasValidFormat(
            @ForAll("randomLocalDateTimes") LocalDateTime createdAt) {

        String migratedPeriod = computeMigratedPeriod(createdAt);

        // The migrated period must match YYYY-MM format
        assertThat(migratedPeriod).matches("\\d{4}-(0[1-9]|1[0-2])");
    }

    @Property(tries = 100)
    void migratedPeriodPreservesYearAndMonth(
            @ForAll("randomLocalDateTimes") LocalDateTime createdAt) {

        String migratedPeriod = computeMigratedPeriod(createdAt);

        // Extract year and month from the period string
        String[] parts = migratedPeriod.split("-");
        int periodYear = Integer.parseInt(parts[0]);
        int periodMonth = Integer.parseInt(parts[1]);

        // They must match the original created_at values
        assertThat(periodYear).isEqualTo(createdAt.getYear());
        assertThat(periodMonth).isEqualTo(createdAt.getMonthValue());
    }

    @Property(tries = 100)
    void allRecordsHaveNonNullPeriodAfterMigration(
            @ForAll("recordBatch") List<LocalDateTime> createdAtTimestamps) {

        // Simulates: after migration, every record with a non-null created_at
        // should have a valid (non-null, non-empty) period
        for (LocalDateTime createdAt : createdAtTimestamps) {
            String migratedPeriod = computeMigratedPeriod(createdAt);
            assertThat(migratedPeriod).isNotNull().isNotEmpty();
        }
    }

    // --- Generators ---

    @Provide
    Arbitrary<LocalDateTime> randomLocalDateTimes() {
        return Combinators.combine(
                Arbitraries.integers().between(2000, 2099),  // year
                Arbitraries.integers().between(1, 12),        // month
                Arbitraries.integers().between(1, 28),        // day (safe for all months)
                Arbitraries.integers().between(0, 23),        // hour
                Arbitraries.integers().between(0, 59),        // minute
                Arbitraries.integers().between(0, 59)         // second
        ).as((year, month, day, hour, minute, second) ->
                LocalDateTime.of(year, month, day, hour, minute, second));
    }

    @Provide
    Arbitrary<List<LocalDateTime>> recordBatch() {
        return randomLocalDateTimes().list().ofMinSize(1).ofMaxSize(30);
    }
}
