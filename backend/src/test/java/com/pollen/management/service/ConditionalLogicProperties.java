package com.pollen.management.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pollen-group-management, Property 8: 条件逻辑评估正确性
 * **Validates: Requirements 3.3**
 *
 * For any 条件逻辑配置和一组字段回答值，字段的可见性应由条件逻辑规则确定性地计算得出。
 * 当条件满足时字段显示（或隐藏，取决于 action），当条件不满足时相反。
 */
class ConditionalLogicProperties {

    private final ConditionalLogicEvaluator evaluator = new ConditionalLogicEvaluator();

    // ---- Generators ----

    @Provide
    Arbitrary<String> fieldKeys() {
        return Arbitraries.of("field_a", "field_b", "field_c", "field_d", "field_e");
    }

    @Provide
    Arbitrary<String> fieldValues() {
        return Arbitraries.of("yes", "no", "maybe", "alpha", "beta", "gamma", "1", "2", "3");
    }

    /**
     * Build a conditional logic JSON with a single EQUALS condition.
     */
    private String buildSingleEqualsJson(String action, String fieldKey, String expectedValue) {
        return String.format(
                "{\"action\":\"%s\",\"logicOperator\":\"AND\",\"conditions\":[" +
                "{\"fieldKey\":\"%s\",\"operator\":\"EQUALS\",\"value\":\"%s\"}" +
                "]}", action, fieldKey, expectedValue);
    }

    /**
     * Build a conditional logic JSON with two EQUALS conditions.
     */
    private String buildTwoEqualsJson(String action, String logicOp,
                                       String key1, String val1,
                                       String key2, String val2) {
        return String.format(
                "{\"action\":\"%s\",\"logicOperator\":\"%s\",\"conditions\":[" +
                "{\"fieldKey\":\"%s\",\"operator\":\"EQUALS\",\"value\":\"%s\"}," +
                "{\"fieldKey\":\"%s\",\"operator\":\"EQUALS\",\"value\":\"%s\"}" +
                "]}", action, logicOp, key1, val1, key2, val2);
    }

    // ---- Property 1: SHOW action visibility ----

    /**
     * SHOW action: field is visible iff the EQUALS condition is met.
     * When the answer matches the expected value, visible=true; otherwise visible=false.
     */
    @Property(tries = 100)
    void showAction_visibleIffConditionMet(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("fieldValues") String expectedValue,
            @ForAll("fieldValues") String actualValue) {

        String json = buildSingleEqualsJson("SHOW", fieldKey, expectedValue);
        Map<String, Object> answers = Map.of(fieldKey, actualValue);

        boolean visible = evaluator.isFieldVisible(json, answers);
        boolean conditionMet = expectedValue.equals(actualValue);

        assertThat(visible).isEqualTo(conditionMet);
    }

    /**
     * HIDE action: field is visible iff the EQUALS condition is NOT met.
     */
    @Property(tries = 100)
    void hideAction_visibleIffConditionNotMet(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("fieldValues") String expectedValue,
            @ForAll("fieldValues") String actualValue) {

        String json = buildSingleEqualsJson("HIDE", fieldKey, expectedValue);
        Map<String, Object> answers = Map.of(fieldKey, actualValue);

        boolean visible = evaluator.isFieldVisible(json, answers);
        boolean conditionMet = expectedValue.equals(actualValue);

        assertThat(visible).isEqualTo(!conditionMet);
    }

    // ---- Property 2: Determinism ----

    /**
     * Same inputs always produce the same output (deterministic evaluation).
     */
    @Property(tries = 100)
    void sameInputsAlwaysProduceSameOutput(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("fieldValues") String expectedValue,
            @ForAll("fieldValues") String actualValue,
            @ForAll @IntRange(min = 2, max = 10) int repetitions) {

        String json = buildSingleEqualsJson("SHOW", fieldKey, expectedValue);
        Map<String, Object> answers = Map.of(fieldKey, actualValue);

        boolean firstResult = evaluator.isFieldVisible(json, answers);
        for (int i = 1; i < repetitions; i++) {
            boolean result = evaluator.isFieldVisible(json, answers);
            assertThat(result)
                    .as("Evaluation #%d should equal first evaluation", i + 1)
                    .isEqualTo(firstResult);
        }
    }

    // ---- Property 3: SHOW and HIDE produce opposite results ----

    /**
     * For the same conditions and answers, SHOW and HIDE actions produce opposite visibility.
     */
    @Property(tries = 100)
    void showAndHideWithSameConditionsProduceOppositeResults(
            @ForAll("fieldKeys") String fieldKey,
            @ForAll("fieldValues") String expectedValue,
            @ForAll("fieldValues") String actualValue) {

        String showJson = buildSingleEqualsJson("SHOW", fieldKey, expectedValue);
        String hideJson = buildSingleEqualsJson("HIDE", fieldKey, expectedValue);
        Map<String, Object> answers = Map.of(fieldKey, actualValue);

        boolean showResult = evaluator.isFieldVisible(showJson, answers);
        boolean hideResult = evaluator.isFieldVisible(hideJson, answers);

        assertThat(showResult).isNotEqualTo(hideResult);
    }

    // ---- Additional: SHOW/HIDE with AND/OR multi-condition ----

    /**
     * SHOW with AND: visible iff ALL conditions met.
     * HIDE with AND: visible iff NOT ALL conditions met.
     * These should be opposites.
     */
    @Property(tries = 100)
    void showAndHideOppositeWithMultipleConditions(
            @ForAll("fieldKeys") String key1,
            @ForAll("fieldValues") String expected1,
            @ForAll("fieldValues") String actual1,
            @ForAll("fieldValues") String expected2,
            @ForAll("fieldValues") String actual2) {

        // Use two distinct keys to avoid collision
        String k2 = key1.equals("field_a") ? "field_b" : "field_a";

        String showJson = buildTwoEqualsJson("SHOW", "AND", key1, expected1, k2, expected2);
        String hideJson = buildTwoEqualsJson("HIDE", "AND", key1, expected1, k2, expected2);

        Map<String, Object> answers = new HashMap<>();
        answers.put(key1, actual1);
        answers.put(k2, actual2);

        boolean showResult = evaluator.isFieldVisible(showJson, answers);
        boolean hideResult = evaluator.isFieldVisible(hideJson, answers);

        assertThat(showResult).isNotEqualTo(hideResult);
    }
}
