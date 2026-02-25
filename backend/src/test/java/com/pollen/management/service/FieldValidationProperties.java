package com.pollen.management.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 9: 字段验证规则执行
 * **Validates: Requirements 3.4, 3.10**
 *
 * For any 字段验证规则配置和输入值，验证结果应确定性地反映输入是否满足所有规则约束。
 * 不满足时应返回包含具体字段标识的错误信息。
 */
class FieldValidationProperties {

    private final FieldValidationService service = new FieldValidationService();

    // ---- Generators ----

    @Provide
    Arbitrary<String> fieldKeys() {
        return Arbitraries.of("name", "age", "email", "score", "birthday", "hobbies");
    }

    // ---- Property: Required field validation ----

    /**
     * Required fields with empty values always produce exactly one error containing the field key.
     * Non-required fields with empty values always pass.
     */
    @Property(tries = 100)
    void requiredFieldValidation(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll boolean required) {

        List<String> errors = service.validate(fieldKey, null, required, null);

        if (required) {
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).contains(fieldKey);
        } else {
            assertThat(errors).isEmpty();
        }
    }

    // ---- Property: minLength / maxLength validation ----

    @Provide
    Arbitrary<String> nonBlankStrings() {
        // Generate strings with at least one non-whitespace char to avoid being treated as empty
        return Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(30)
                .alpha()
                .ofMinLength(1);
    }

    /**
     * For any non-blank string value and minLength/maxLength rules, validation passes iff
     * the string length is within [minLength, maxLength].
     * Errors always contain the field key.
     */
    @Property(tries = 100)
    void lengthRuleValidation(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll @IntRange(min = 1, max = 10) int minLen,
            @ForAll @IntRange(min = 1, max = 20) int extraMax,
            @ForAll("nonBlankStrings") String value) {

        int maxLen = minLen + extraMax;
        String rules = String.format("{\"minLength\": %d, \"maxLength\": %d}", minLen, maxLen);

        List<String> errors = service.validate(fieldKey, value, false, rules);

        boolean tooShort = value.length() < minLen;
        boolean tooLong = value.length() > maxLen;

        if (!tooShort && !tooLong) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).allMatch(e -> e.contains(fieldKey));
        }
    }

    // ---- Property: min / max numeric validation ----

    /**
     * For any numeric value and min/max rules, validation passes iff
     * the value is within [min, max]. Errors contain the field key.
     */
    @Property(tries = 100)
    void numericRangeValidation(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll @IntRange(min = -100, max = 100) int minVal,
            @ForAll @IntRange(min = 1, max = 200) int range,
            @ForAll @IntRange(min = -300, max = 300) int value) {

        int maxVal = minVal + range;
        String rules = String.format("{\"min\": %d, \"max\": %d}", minVal, maxVal);

        List<String> errors = service.validate(fieldKey, value, false, rules);

        boolean belowMin = value < minVal;
        boolean aboveMax = value > maxVal;

        if (!belowMin && !aboveMax) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).allMatch(e -> e.contains(fieldKey));
        }
    }

    // ---- Property: pattern validation ----

    /**
     * For any non-blank string value, validation against a digits-only pattern passes iff
     * the value matches the pattern. Errors contain the field key.
     */
    @Property(tries = 100)
    void patternValidation(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("nonBlankStrings") String value) {

        String pattern = "^[a-z]+$";
        String rules = String.format("{\"pattern\": \"%s\"}", pattern);

        List<String> errors = service.validate(fieldKey, value, false, rules);

        boolean matches = value.matches(pattern);

        if (matches) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).contains(fieldKey);
        }
    }

    // ---- Property: date range validation ----

    /**
     * For any date value within a generated range, validation passes iff
     * the date is within [minDate, maxDate]. Errors contain the field key.
     */
    @Property(tries = 100)
    void dateRangeValidation(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll @IntRange(min = 2000, max = 2020) int minYear,
            @ForAll @IntRange(min = 1, max = 10) int yearRange,
            @ForAll @IntRange(min = 1990, max = 2035) int testYear,
            @ForAll @IntRange(min = 1, max = 12) int month,
            @ForAll @IntRange(min = 1, max = 28) int day) {

        int maxYear = minYear + yearRange;
        String minDate = String.format("%04d-01-01", minYear);
        String maxDate = String.format("%04d-12-31", maxYear);
        String testDate = String.format("%04d-%02d-%02d", testYear, month, day);

        String rules = String.format("{\"minDate\": \"%s\", \"maxDate\": \"%s\"}", minDate, maxDate);

        List<String> errors = service.validate(fieldKey, testDate, false, rules);

        LocalDate min = LocalDate.parse(minDate);
        LocalDate max = LocalDate.parse(maxDate);
        LocalDate test = LocalDate.parse(testDate);

        boolean beforeMin = test.isBefore(min);
        boolean afterMax = test.isAfter(max);

        if (!beforeMin && !afterMax) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).allMatch(e -> e.contains(fieldKey));
        }
    }

    // ---- Property: minSelect / maxSelect validation ----

    /**
     * For any list of selections and minSelect/maxSelect rules, validation passes iff
     * the selection count is within [minSelect, maxSelect]. Errors contain the field key.
     */
    @Property(tries = 100)
    void selectionCountValidation(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll @IntRange(min = 1, max = 3) int minSelect,
            @ForAll @IntRange(min = 1, max = 5) int extraMax,
            @ForAll @IntRange(min = 0, max = 10) int selectionCount) {

        int maxSelect = minSelect + extraMax;
        String rules = String.format("{\"minSelect\": %d, \"maxSelect\": %d}", minSelect, maxSelect);

        List<String> items = new ArrayList<>();
        for (int i = 0; i < selectionCount; i++) {
            items.add("item_" + i);
        }

        // Empty list is treated as empty value; skip required check by using required=false
        // but empty collections are considered empty and skip validation
        if (items.isEmpty()) {
            // Empty collection with required=false skips validation entirely
            List<String> errors = service.validate(fieldKey, items, false, rules);
            assertThat(errors).isEmpty();
        } else {
            List<String> errors = service.validate(fieldKey, items, false, rules);

            boolean tooFew = items.size() < minSelect;
            boolean tooMany = items.size() > maxSelect;

            if (!tooFew && !tooMany) {
                assertThat(errors).isEmpty();
            } else {
                assertThat(errors).isNotEmpty();
                assertThat(errors).allMatch(e -> e.contains(fieldKey));
            }
        }
    }

    // ---- Property: Determinism ----

    /**
     * Same inputs always produce the same validation result (deterministic).
     */
    @Property(tries = 100)
    void validationIsDeterministic(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("nonBlankStrings") String value,
            @ForAll boolean required,
            @ForAll @IntRange(min = 2, max = 5) int repetitions) {

        String rules = "{\"minLength\": 3, \"maxLength\": 15}";

        List<String> firstResult = service.validate(fieldKey, value, required, rules);
        for (int i = 1; i < repetitions; i++) {
            List<String> result = service.validate(fieldKey, value, required, rules);
            assertThat(result)
                    .as("Validation run #%d should equal first run", i + 1)
                    .isEqualTo(firstResult);
        }
    }

    // ---- Property: Error messages always contain field key ----

    /**
     * Every error message returned by validation must contain the field key,
     * ensuring callers can identify which field failed.
     */
    @Property(tries = 100)
    void errorMessagesAlwaysContainFieldKey(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("nonBlankStrings") String value) {

        // Use rules that will likely produce errors for short alpha strings
        String rules = "{\"minLength\": 10, \"pattern\": \"^[0-9]+$\"}";

        List<String> errors = service.validate(fieldKey, value, false, rules);

        assertThat(errors).allMatch(e -> e.contains(fieldKey));
    }
}
