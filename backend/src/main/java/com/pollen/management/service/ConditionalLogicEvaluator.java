package com.pollen.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 条件逻辑评估引擎
 * 根据条件逻辑配置和用户回答计算字段可见性。
 * 支持 SHOW/HIDE action，AND/OR 逻辑运算符，
 * 以及 EQUALS、NOT_EQUALS、CONTAINS、GREATER_THAN、LESS_THAN、IN、NOT_IN 操作符。
 */
@Component
public class ConditionalLogicEvaluator {

    private final ObjectMapper objectMapper;

    public ConditionalLogicEvaluator() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 评估字段是否可见。
     *
     * @param conditionalLogicJson 条件逻辑 JSON 配置
     * @param answers              用户回答 map（fieldKey -> 回答值）
     * @return true 表示字段可见，false 表示字段隐藏
     */
    public boolean isFieldVisible(String conditionalLogicJson, Map<String, Object> answers) {
        if (conditionalLogicJson == null || conditionalLogicJson.isBlank()) {
            return true;
        }

        try {
            JsonNode root = objectMapper.readTree(conditionalLogicJson);

            String action = root.path("action").asText("SHOW").toUpperCase();
            String logicOperator = root.path("logicOperator").asText("AND").toUpperCase();
            JsonNode conditionsNode = root.path("conditions");

            if (!conditionsNode.isArray() || conditionsNode.isEmpty()) {
                return true;
            }

            boolean conditionsMet = evaluateConditions(conditionsNode, logicOperator, answers);

            if ("SHOW".equals(action)) {
                return conditionsMet;
            } else if ("HIDE".equals(action)) {
                return !conditionsMet;
            }
            // Unknown action defaults to visible
            return true;

        } catch (JsonProcessingException e) {
            // Invalid JSON — treat as no conditional logic, field is visible
            return true;
        }
    }

    private boolean evaluateConditions(JsonNode conditionsNode, String logicOperator, Map<String, Object> answers) {
        if ("AND".equals(logicOperator)) {
            for (JsonNode condition : conditionsNode) {
                if (!evaluateSingleCondition(condition, answers)) {
                    return false;
                }
            }
            return true;
        } else {
            // OR
            for (JsonNode condition : conditionsNode) {
                if (evaluateSingleCondition(condition, answers)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean evaluateSingleCondition(JsonNode condition, Map<String, Object> answers) {
        String fieldKey = condition.path("fieldKey").asText();
        String operator = condition.path("operator").asText().toUpperCase();
        JsonNode valueNode = condition.path("value");

        Object answerValue = answers.get(fieldKey);

        return switch (operator) {
            case "EQUALS" -> evaluateEquals(answerValue, valueNode);
            case "NOT_EQUALS" -> !evaluateEquals(answerValue, valueNode);
            case "CONTAINS" -> evaluateContains(answerValue, valueNode);
            case "GREATER_THAN" -> evaluateComparison(answerValue, valueNode) > 0;
            case "LESS_THAN" -> evaluateComparison(answerValue, valueNode) < 0;
            case "IN" -> evaluateIn(answerValue, valueNode);
            case "NOT_IN" -> !evaluateIn(answerValue, valueNode);
            default -> false;
        };
    }

    private boolean evaluateEquals(Object answerValue, JsonNode valueNode) {
        if (answerValue == null) {
            return valueNode.isNull();
        }
        String answerStr = String.valueOf(answerValue);
        String expectedStr = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
        return answerStr.equals(expectedStr);
    }

    private boolean evaluateContains(Object answerValue, JsonNode valueNode) {
        if (answerValue == null) {
            return false;
        }
        String expected = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();

        if (answerValue instanceof List<?> list) {
            // If answer is a list, check if it contains the expected value
            return list.stream().anyMatch(item -> String.valueOf(item).equals(expected));
        }
        // If answer is a string, check if it contains the expected substring
        return String.valueOf(answerValue).contains(expected);
    }

    private int evaluateComparison(Object answerValue, JsonNode valueNode) {
        if (answerValue == null) {
            return -1; // null is considered less than any value
        }
        try {
            double answerNum = toDouble(answerValue);
            double expectedNum = valueNode.asDouble();
            return Double.compare(answerNum, expectedNum);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            String answerStr = String.valueOf(answerValue);
            String expectedStr = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
            return answerStr.compareTo(expectedStr);
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private boolean evaluateIn(Object answerValue, JsonNode valueNode) {
        if (answerValue == null || !valueNode.isArray()) {
            return false;
        }
        String answerStr = String.valueOf(answerValue);
        for (JsonNode element : valueNode) {
            String elementStr = element.isTextual() ? element.asText() : element.toString();
            if (answerStr.equals(elementStr)) {
                return true;
            }
        }
        return false;
    }
}
