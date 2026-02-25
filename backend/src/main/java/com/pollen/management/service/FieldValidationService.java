package com.pollen.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 字段验证服务
 * 根据验证规则配置校验输入值，支持 minLength、maxLength、pattern、
 * min、max、minDate、maxDate、minSelect、maxSelect 以及必填校验。
 * 返回包含具体字段标识的错误信息列表。
 */
@Component
public class FieldValidationService {

    private final ObjectMapper objectMapper;

    public FieldValidationService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 校验字段值。
     *
     * @param fieldKey            字段标识
     * @param value               输入值
     * @param required            是否必填
     * @param validationRulesJson 验证规则 JSON 字符串
     * @return 错误信息列表（空列表表示验证通过）
     */
    public List<String> validate(String fieldKey, Object value, boolean required, String validationRulesJson) {
        List<String> errors = new ArrayList<>();

        // Required check
        if (required && isEmpty(value)) {
            errors.add(fieldKey + ": 此字段为必填项");
            return errors;
        }

        // If value is empty and not required, skip further validation
        if (isEmpty(value)) {
            return errors;
        }

        // No rules to validate
        if (validationRulesJson == null || validationRulesJson.isBlank()) {
            return errors;
        }

        JsonNode rules;
        try {
            rules = objectMapper.readTree(validationRulesJson);
        } catch (Exception e) {
            // Invalid JSON rules — skip validation
            return errors;
        }

        String customMessage = rules.has("customMessage") ? rules.get("customMessage").asText() : null;

        validateMinLength(fieldKey, value, rules, errors, customMessage);
        validateMaxLength(fieldKey, value, rules, errors, customMessage);
        validatePattern(fieldKey, value, rules, errors, customMessage);
        validateMin(fieldKey, value, rules, errors, customMessage);
        validateMax(fieldKey, value, rules, errors, customMessage);
        validateMinDate(fieldKey, value, rules, errors, customMessage);
        validateMaxDate(fieldKey, value, rules, errors, customMessage);
        validateMinSelect(fieldKey, value, rules, errors, customMessage);
        validateMaxSelect(fieldKey, value, rules, errors, customMessage);

        return errors;
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.trim().isEmpty();
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        return false;
    }

    private void validateMinLength(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("minLength")) return;
        int minLength = rules.get("minLength").asInt();
        String strValue = String.valueOf(value);
        if (strValue.length() < minLength) {
            errors.add(customMessage != null ? fieldKey + ": " + customMessage
                    : fieldKey + ": 长度不能少于" + minLength + "个字符");
        }
    }

    private void validateMaxLength(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("maxLength")) return;
        int maxLength = rules.get("maxLength").asInt();
        String strValue = String.valueOf(value);
        if (strValue.length() > maxLength) {
            errors.add(customMessage != null ? fieldKey + ": " + customMessage
                    : fieldKey + ": 长度不能超过" + maxLength + "个字符");
        }
    }

    private void validatePattern(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("pattern")) return;
        String pattern = rules.get("pattern").asText();
        String strValue = String.valueOf(value);
        if (!strValue.matches(pattern)) {
            errors.add(customMessage != null ? fieldKey + ": " + customMessage
                    : fieldKey + ": 格式不正确");
        }
    }

    private void validateMin(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("min")) return;
        try {
            double min = rules.get("min").asDouble();
            double numValue = toDouble(value);
            if (numValue < min) {
                errors.add(customMessage != null ? fieldKey + ": " + customMessage
                        : fieldKey + ": 值不能小于" + formatNumber(min));
            }
        } catch (NumberFormatException e) {
            errors.add(fieldKey + ": 值必须为数字");
        }
    }

    private void validateMax(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("max")) return;
        try {
            double max = rules.get("max").asDouble();
            double numValue = toDouble(value);
            if (numValue > max) {
                errors.add(customMessage != null ? fieldKey + ": " + customMessage
                        : fieldKey + ": 值不能大于" + formatNumber(max));
            }
        } catch (NumberFormatException e) {
            errors.add(fieldKey + ": 值必须为数字");
        }
    }

    private void validateMinDate(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("minDate")) return;
        try {
            LocalDate minDate = LocalDate.parse(rules.get("minDate").asText());
            LocalDate dateValue = LocalDate.parse(String.valueOf(value));
            if (dateValue.isBefore(minDate)) {
                errors.add(customMessage != null ? fieldKey + ": " + customMessage
                        : fieldKey + ": 日期不能早于" + minDate);
            }
        } catch (DateTimeParseException e) {
            errors.add(fieldKey + ": 日期格式不正确");
        }
    }

    private void validateMaxDate(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("maxDate")) return;
        try {
            LocalDate maxDate = LocalDate.parse(rules.get("maxDate").asText());
            LocalDate dateValue = LocalDate.parse(String.valueOf(value));
            if (dateValue.isAfter(maxDate)) {
                errors.add(customMessage != null ? fieldKey + ": " + customMessage
                        : fieldKey + ": 日期不能晚于" + maxDate);
            }
        } catch (DateTimeParseException e) {
            errors.add(fieldKey + ": 日期格式不正确");
        }
    }

    private void validateMinSelect(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("minSelect")) return;
        int minSelect = rules.get("minSelect").asInt();
        int count = getSelectionCount(value);
        if (count < minSelect) {
            errors.add(customMessage != null ? fieldKey + ": " + customMessage
                    : fieldKey + ": 至少选择" + minSelect + "项");
        }
    }

    private void validateMaxSelect(String fieldKey, Object value, JsonNode rules, List<String> errors, String customMessage) {
        if (!rules.has("maxSelect")) return;
        int maxSelect = rules.get("maxSelect").asInt();
        int count = getSelectionCount(value);
        if (count > maxSelect) {
            errors.add(customMessage != null ? fieldKey + ": " + customMessage
                    : fieldKey + ": 最多选择" + maxSelect + "项");
        }
    }

    private int getSelectionCount(Object value) {
        if (value instanceof Collection<?> c) {
            return c.size();
        }
        // Single value counts as 1
        return 1;
    }

    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
