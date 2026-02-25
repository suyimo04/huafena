package com.pollen.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalLogicEvaluatorTest {

    private ConditionalLogicEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionalLogicEvaluator();
    }

    // --- Null / empty / invalid JSON → always visible ---

    @Test
    void isFieldVisible_nullJson_returnsTrue() {
        assertTrue(evaluator.isFieldVisible(null, Map.of()));
    }

    @Test
    void isFieldVisible_emptyJson_returnsTrue() {
        assertTrue(evaluator.isFieldVisible("", Map.of()));
    }

    @Test
    void isFieldVisible_blankJson_returnsTrue() {
        assertTrue(evaluator.isFieldVisible("   ", Map.of()));
    }

    @Test
    void isFieldVisible_invalidJson_returnsTrue() {
        assertTrue(evaluator.isFieldVisible("{bad json", Map.of("a", "b")));
    }

    @Test
    void isFieldVisible_emptyConditionsArray_returnsTrue() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[]}
                """;
        assertTrue(evaluator.isFieldVisible(json, Map.of()));
    }

    // --- EQUALS operator ---

    @Test
    void equals_matchingStringValue_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"is_student","operator":"EQUALS","value":"yes"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("is_student", "yes")));
    }

    @Test
    void equals_nonMatchingStringValue_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"is_student","operator":"EQUALS","value":"yes"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("is_student", "no")));
    }

    @Test
    void equals_numericValue_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"age","operator":"EQUALS","value":"25"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("age", "25")));
    }

    @Test
    void equals_missingAnswer_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"missing","operator":"EQUALS","value":"yes"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of()));
    }

    // --- NOT_EQUALS operator ---

    @Test
    void notEquals_differentValue_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"role","operator":"NOT_EQUALS","value":"admin"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("role", "member")));
    }

    @Test
    void notEquals_sameValue_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"role","operator":"NOT_EQUALS","value":"admin"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("role", "admin")));
    }

    // --- CONTAINS operator ---

    @Test
    void contains_substringMatch_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"bio","operator":"CONTAINS","value":"花粉"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("bio", "我是花粉小组成员")));
    }

    @Test
    void contains_noSubstringMatch_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"bio","operator":"CONTAINS","value":"花粉"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("bio", "普通用户")));
    }

    @Test
    void contains_listAnswer_containsValue() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"hobbies","operator":"CONTAINS","value":"coding"}
                ]}""";
        Map<String, Object> answers = new HashMap<>();
        answers.put("hobbies", List.of("coding", "reading"));
        assertTrue(evaluator.isFieldVisible(json, answers));
    }

    @Test
    void contains_listAnswer_doesNotContainValue() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"hobbies","operator":"CONTAINS","value":"gaming"}
                ]}""";
        Map<String, Object> answers = new HashMap<>();
        answers.put("hobbies", List.of("coding", "reading"));
        assertFalse(evaluator.isFieldVisible(json, answers));
    }

    @Test
    void contains_nullAnswer_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"bio","operator":"CONTAINS","value":"test"}
                ]}""";
        Map<String, Object> answers = new HashMap<>();
        answers.put("bio", null);
        assertFalse(evaluator.isFieldVisible(json, answers));
    }

    // --- GREATER_THAN operator ---

    @Test
    void greaterThan_largerNumber_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"age","operator":"GREATER_THAN","value":18}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("age", 25)));
    }

    @Test
    void greaterThan_equalNumber_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"age","operator":"GREATER_THAN","value":18}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("age", 18)));
    }

    @Test
    void greaterThan_smallerNumber_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"age","operator":"GREATER_THAN","value":18}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("age", 15)));
    }

    // --- LESS_THAN operator ---

    @Test
    void lessThan_smallerNumber_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"score","operator":"LESS_THAN","value":60}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("score", 45)));
    }

    @Test
    void lessThan_equalNumber_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"score","operator":"LESS_THAN","value":60}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("score", 60)));
    }

    // --- IN operator ---

    @Test
    void in_valueInArray_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"city","operator":"IN","value":["北京","上海","广州"]}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("city", "上海")));
    }

    @Test
    void in_valueNotInArray_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"city","operator":"IN","value":["北京","上海","广州"]}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("city", "深圳")));
    }

    @Test
    void in_nullAnswer_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"city","operator":"IN","value":["北京","上海"]}
                ]}""";
        Map<String, Object> answers = new HashMap<>();
        answers.put("city", null);
        assertFalse(evaluator.isFieldVisible(json, answers));
    }

    // --- NOT_IN operator ---

    @Test
    void notIn_valueNotInArray_conditionMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"city","operator":"NOT_IN","value":["北京","上海"]}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("city", "深圳")));
    }

    @Test
    void notIn_valueInArray_conditionNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"city","operator":"NOT_IN","value":["北京","上海"]}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("city", "北京")));
    }

    // --- SHOW action ---

    @Test
    void showAction_conditionsMet_visible() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"type","operator":"EQUALS","value":"A"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("type", "A")));
    }

    @Test
    void showAction_conditionsNotMet_hidden() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"type","operator":"EQUALS","value":"A"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("type", "B")));
    }

    // --- HIDE action ---

    @Test
    void hideAction_conditionsMet_hidden() {
        String json = """
                {"action":"HIDE","logicOperator":"AND","conditions":[
                  {"fieldKey":"type","operator":"EQUALS","value":"A"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("type", "A")));
    }

    @Test
    void hideAction_conditionsNotMet_visible() {
        String json = """
                {"action":"HIDE","logicOperator":"AND","conditions":[
                  {"fieldKey":"type","operator":"EQUALS","value":"A"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("type", "B")));
    }

    // --- AND logic operator ---

    @Test
    void andLogic_allConditionsMet_conditionsMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"is_student","operator":"EQUALS","value":"yes"},
                  {"fieldKey":"age","operator":"GREATER_THAN","value":18}
                ]}""";
        Map<String, Object> answers = Map.of("is_student", "yes", "age", 25);
        assertTrue(evaluator.isFieldVisible(json, answers));
    }

    @Test
    void andLogic_oneConditionNotMet_conditionsNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"AND","conditions":[
                  {"fieldKey":"is_student","operator":"EQUALS","value":"yes"},
                  {"fieldKey":"age","operator":"GREATER_THAN","value":18}
                ]}""";
        Map<String, Object> answers = Map.of("is_student", "yes", "age", 16);
        assertFalse(evaluator.isFieldVisible(json, answers));
    }

    // --- OR logic operator ---

    @Test
    void orLogic_oneConditionMet_conditionsMet() {
        String json = """
                {"action":"SHOW","logicOperator":"OR","conditions":[
                  {"fieldKey":"role","operator":"EQUALS","value":"admin"},
                  {"fieldKey":"role","operator":"EQUALS","value":"leader"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("role", "leader")));
    }

    @Test
    void orLogic_noConditionMet_conditionsNotMet() {
        String json = """
                {"action":"SHOW","logicOperator":"OR","conditions":[
                  {"fieldKey":"role","operator":"EQUALS","value":"admin"},
                  {"fieldKey":"role","operator":"EQUALS","value":"leader"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("role", "member")));
    }

    // --- Combined: HIDE + OR ---

    @Test
    void hideWithOr_oneConditionMet_hidden() {
        String json = """
                {"action":"HIDE","logicOperator":"OR","conditions":[
                  {"fieldKey":"status","operator":"EQUALS","value":"banned"},
                  {"fieldKey":"status","operator":"EQUALS","value":"suspended"}
                ]}""";
        assertFalse(evaluator.isFieldVisible(json, Map.of("status", "banned")));
    }

    @Test
    void hideWithOr_noConditionMet_visible() {
        String json = """
                {"action":"HIDE","logicOperator":"OR","conditions":[
                  {"fieldKey":"status","operator":"EQUALS","value":"banned"},
                  {"fieldKey":"status","operator":"EQUALS","value":"suspended"}
                ]}""";
        assertTrue(evaluator.isFieldVisible(json, Map.of("status", "active")));
    }

    // --- Real-world scenario from design doc ---

    @Test
    void realWorldScenario_showSchoolWhenStudent() {
        String json = """
                {"action":"SHOW","conditions":[
                  {"fieldKey":"is_student","operator":"EQUALS","value":"yes"}
                ]}""";
        // Student → school field visible
        assertTrue(evaluator.isFieldVisible(json, Map.of("is_student", "yes")));
        // Not student → school field hidden
        assertFalse(evaluator.isFieldVisible(json, Map.of("is_student", "no")));
    }

    // --- Default logicOperator (AND when missing) ---

    @Test
    void missingLogicOperator_defaultsToAnd() {
        String json = """
                {"action":"SHOW","conditions":[
                  {"fieldKey":"a","operator":"EQUALS","value":"1"},
                  {"fieldKey":"b","operator":"EQUALS","value":"2"}
                ]}""";
        // Both met → visible
        assertTrue(evaluator.isFieldVisible(json, Map.of("a", "1", "b", "2")));
        // Only one met → hidden (AND behavior)
        assertFalse(evaluator.isFieldVisible(json, Map.of("a", "1", "b", "3")));
    }
}
