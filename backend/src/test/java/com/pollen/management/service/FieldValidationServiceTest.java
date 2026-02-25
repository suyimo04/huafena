package com.pollen.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldValidationServiceTest {

    private FieldValidationService service;

    @BeforeEach
    void setUp() {
        service = new FieldValidationService();
    }

    // === Required field tests ===

    @Test
    void requiredField_nullValue_returnsError() {
        List<String> errors = service.validate("name", null, true, null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("name"));
        assertTrue(errors.get(0).contains("必填"));
    }

    @Test
    void requiredField_emptyString_returnsError() {
        List<String> errors = service.validate("name", "  ", true, null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("name"));
    }

    @Test
    void requiredField_emptyCollection_returnsError() {
        List<String> errors = service.validate("tags", Collections.emptyList(), true, null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("tags"));
    }

    @Test
    void requiredField_withValue_noError() {
        List<String> errors = service.validate("name", "Alice", true, null);
        assertTrue(errors.isEmpty());
    }

    @Test
    void optionalField_nullValue_noError() {
        List<String> errors = service.validate("name", null, false, "{\"minLength\": 2}");
        assertTrue(errors.isEmpty());
    }

    // === minLength / maxLength tests ===

    @Test
    void minLength_tooShort_returnsError() {
        String rules = "{\"minLength\": 3}";
        List<String> errors = service.validate("name", "ab", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("name"));
        assertTrue(errors.get(0).contains("3"));
    }

    @Test
    void minLength_exactLength_noError() {
        String rules = "{\"minLength\": 3}";
        List<String> errors = service.validate("name", "abc", false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void maxLength_tooLong_returnsError() {
        String rules = "{\"maxLength\": 5}";
        List<String> errors = service.validate("name", "abcdef", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("name"));
        assertTrue(errors.get(0).contains("5"));
    }

    @Test
    void maxLength_exactLength_noError() {
        String rules = "{\"maxLength\": 5}";
        List<String> errors = service.validate("name", "abcde", false, rules);
        assertTrue(errors.isEmpty());
    }

    // === pattern tests ===

    @Test
    void pattern_matches_noError() {
        String rules = "{\"pattern\": \"^[a-zA-Z0-9]+$\"}";
        List<String> errors = service.validate("code", "abc123", false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void pattern_noMatch_returnsError() {
        String rules = "{\"pattern\": \"^[a-zA-Z0-9]+$\"}";
        List<String> errors = service.validate("code", "abc 123!", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("code"));
        assertTrue(errors.get(0).contains("格式"));
    }

    // === min / max tests ===

    @Test
    void min_belowMinimum_returnsError() {
        String rules = "{\"min\": 10}";
        List<String> errors = service.validate("age", 5, false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("age"));
        assertTrue(errors.get(0).contains("10"));
    }

    @Test
    void min_exactMinimum_noError() {
        String rules = "{\"min\": 10}";
        List<String> errors = service.validate("age", 10, false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void max_aboveMaximum_returnsError() {
        String rules = "{\"max\": 100}";
        List<String> errors = service.validate("age", 150, false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("age"));
        assertTrue(errors.get(0).contains("100"));
    }

    @Test
    void max_exactMaximum_noError() {
        String rules = "{\"max\": 100}";
        List<String> errors = service.validate("age", 100, false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void min_stringNumber_parsedCorrectly() {
        String rules = "{\"min\": 0, \"max\": 150}";
        List<String> errors = service.validate("score", "75", false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void min_nonNumericValue_returnsError() {
        String rules = "{\"min\": 0}";
        List<String> errors = service.validate("score", "abc", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("score"));
        assertTrue(errors.get(0).contains("数字"));
    }

    // === minDate / maxDate tests ===

    @Test
    void minDate_beforeMinDate_returnsError() {
        String rules = "{\"minDate\": \"2000-01-01\"}";
        List<String> errors = service.validate("birthday", "1999-12-31", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("birthday"));
        assertTrue(errors.get(0).contains("2000-01-01"));
    }

    @Test
    void minDate_exactMinDate_noError() {
        String rules = "{\"minDate\": \"2000-01-01\"}";
        List<String> errors = service.validate("birthday", "2000-01-01", false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void maxDate_afterMaxDate_returnsError() {
        String rules = "{\"maxDate\": \"2025-12-31\"}";
        List<String> errors = service.validate("birthday", "2026-01-01", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("birthday"));
        assertTrue(errors.get(0).contains("2025-12-31"));
    }

    @Test
    void maxDate_exactMaxDate_noError() {
        String rules = "{\"maxDate\": \"2025-12-31\"}";
        List<String> errors = service.validate("birthday", "2025-12-31", false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void date_invalidFormat_returnsError() {
        String rules = "{\"minDate\": \"2000-01-01\"}";
        List<String> errors = service.validate("birthday", "not-a-date", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("birthday"));
        assertTrue(errors.get(0).contains("日期格式"));
    }

    // === minSelect / maxSelect tests ===

    @Test
    void minSelect_tooFewSelections_returnsError() {
        String rules = "{\"minSelect\": 2}";
        List<String> errors = service.validate("hobbies", Arrays.asList("reading"), false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("hobbies"));
        assertTrue(errors.get(0).contains("2"));
    }

    @Test
    void minSelect_exactMinSelections_noError() {
        String rules = "{\"minSelect\": 2}";
        List<String> errors = service.validate("hobbies", Arrays.asList("reading", "coding"), false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void maxSelect_tooManySelections_returnsError() {
        String rules = "{\"maxSelect\": 2}";
        List<String> errors = service.validate("hobbies", Arrays.asList("a", "b", "c"), false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("hobbies"));
        assertTrue(errors.get(0).contains("2"));
    }

    @Test
    void maxSelect_exactMaxSelections_noError() {
        String rules = "{\"maxSelect\": 2}";
        List<String> errors = service.validate("hobbies", Arrays.asList("a", "b"), false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void minSelect_singleValue_countsAsOne() {
        String rules = "{\"minSelect\": 1}";
        List<String> errors = service.validate("choice", "option1", false, rules);
        assertTrue(errors.isEmpty());
    }

    // === customMessage tests ===

    @Test
    void customMessage_usedInError() {
        String rules = "{\"minLength\": 5, \"customMessage\": \"自定义错误提示\"}";
        List<String> errors = service.validate("name", "ab", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("自定义错误提示"));
        assertTrue(errors.get(0).contains("name"));
    }

    // === Combined rules tests ===

    @Test
    void multipleRules_allPass_noErrors() {
        String rules = "{\"minLength\": 2, \"maxLength\": 10, \"pattern\": \"^[a-zA-Z]+$\"}";
        List<String> errors = service.validate("name", "Alice", false, rules);
        assertTrue(errors.isEmpty());
    }

    @Test
    void multipleRules_multipleFail_multipleErrors() {
        String rules = "{\"minLength\": 10, \"pattern\": \"^[0-9]+$\"}";
        List<String> errors = service.validate("code", "abc", false, rules);
        assertEquals(2, errors.size());
    }

    // === Edge cases ===

    @Test
    void nullRules_noError() {
        List<String> errors = service.validate("name", "value", false, null);
        assertTrue(errors.isEmpty());
    }

    @Test
    void emptyRules_noError() {
        List<String> errors = service.validate("name", "value", false, "");
        assertTrue(errors.isEmpty());
    }

    @Test
    void invalidJsonRules_noError() {
        List<String> errors = service.validate("name", "value", false, "not json");
        assertTrue(errors.isEmpty());
    }

    @Test
    void errorMessages_containFieldKey() {
        String rules = "{\"minLength\": 100}";
        List<String> errors = service.validate("my_field", "short", false, rules);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("my_field:"));
    }
}
