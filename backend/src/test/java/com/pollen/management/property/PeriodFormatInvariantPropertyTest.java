package com.pollen.management.property;

import com.pollen.management.util.PeriodUtils;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: salary-period, Property 1: 周期格式不变量
 * **Validates: Requirements 1.1**
 *
 * For any SalaryRecord 实体，其 period 字段的值必须匹配正则表达式
 * \d{4}-(0[1-9]|1[0-2])（即合法的 "YYYY-MM" 格式）。
 */
class PeriodFormatInvariantPropertyTest {

    private static final String PERIOD_REGEX = "\\d{4}-(0[1-9]|1[0-2])";

    // --- Property: currentPeriod() always returns a valid period format ---

    @Example
    void currentPeriodAlwaysMatchesValidFormat() {
        String period = PeriodUtils.currentPeriod();

        assertThat(period).matches(PERIOD_REGEX);
    }

    // --- Property: isValidPeriod accepts all well-formed YYYY-MM strings ---

    @Property(tries = 100)
    void isValidPeriodAcceptsAllValidYearMonthCombinations(
            @ForAll @IntRange(min = 0, max = 9999) int year,
            @ForAll @IntRange(min = 1, max = 12) int month) {
        String period = String.format("%04d-%02d", year, month);

        assertThat(PeriodUtils.isValidPeriod(period)).isTrue();
        assertThat(period).matches(PERIOD_REGEX);
    }

    // --- Property: isValidPeriod rejects months outside 01-12 ---

    @Property(tries = 100)
    void isValidPeriodRejectsInvalidMonths(
            @ForAll @IntRange(min = 0, max = 9999) int year,
            @ForAll("invalidMonths") int month) {
        String period = String.format("%04d-%02d", year, month);

        assertThat(PeriodUtils.isValidPeriod(period)).isFalse();
    }

    @Provide
    Arbitrary<Integer> invalidMonths() {
        return Arbitraries.oneOf(
                Arbitraries.just(0),
                Arbitraries.integers().between(13, 99)
        );
    }

    // --- Property: isValidPeriod rejects malformed strings ---

    @Property(tries = 100)
    void isValidPeriodRejectsMalformedStrings(
            @ForAll("malformedPeriods") String input) {
        assertThat(PeriodUtils.isValidPeriod(input)).isFalse();
    }

    @Provide
    Arbitrary<String> malformedPeriods() {
        return Arbitraries.oneOf(
                // Missing dash
                Arbitraries.integers().between(0, 9999)
                        .map(y -> String.format("%04d%02d", y, 1)),
                // Wrong separator
                Arbitraries.integers().between(0, 9999)
                        .map(y -> String.format("%04d/%02d", y, 6)),
                // Too short year
                Arbitraries.integers().between(0, 999)
                        .map(y -> String.format("%d-%02d", y, 3)),
                // Extra characters appended
                Arbitraries.integers().between(2000, 2030)
                        .map(y -> String.format("%04d-%02d-01", y, 7)),
                // Empty and blank strings
                Arbitraries.of("", " ", "  "),
                // Random alphabetic noise
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        );
    }

    // --- Property: isValidPeriod rejects null ---

    @Example
    void isValidPeriodRejectsNull() {
        assertThat(PeriodUtils.isValidPeriod(null)).isFalse();
    }

    // --- Property: currentPeriod output is always accepted by isValidPeriod ---

    @Example
    void currentPeriodIsAlwaysAcceptedByIsValidPeriod() {
        String period = PeriodUtils.currentPeriod();

        assertThat(PeriodUtils.isValidPeriod(period)).isTrue();
    }
}
