import type {
  ConditionalLogic,
  ConditionalLogicCondition,
  QuestionnaireField,
  ValidationRules,
} from '@/types/questionnaire'

/**
 * Evaluate a single condition against the current answers.
 */
export function evaluateCondition(
  condition: ConditionalLogicCondition,
  answers: Record<string, unknown>,
): boolean {
  const fieldValue = answers[condition.fieldKey]
  const expected = condition.value

  switch (condition.operator) {
    case 'EQUALS':
      return String(fieldValue ?? '') === String(expected)
    case 'NOT_EQUALS':
      return String(fieldValue ?? '') !== String(expected)
    case 'CONTAINS':
      if (Array.isArray(fieldValue)) {
        return fieldValue.some((v) => String(v) === String(expected))
      }
      return String(fieldValue ?? '').includes(String(expected))
    case 'GREATER_THAN':
      return Number(fieldValue) > Number(expected)
    case 'LESS_THAN':
      return Number(fieldValue) < Number(expected)
    case 'IN':
      if (Array.isArray(expected)) {
        return expected.includes(String(fieldValue ?? ''))
      }
      return false
    case 'NOT_IN':
      if (Array.isArray(expected)) {
        return !expected.includes(String(fieldValue ?? ''))
      }
      return true
    default:
      return false
  }
}

/**
 * Evaluate conditional logic to determine if a field should be visible.
 * Returns true if the field should be visible.
 */
export function evaluateConditionalLogic(
  logic: ConditionalLogic | null,
  answers: Record<string, unknown>,
): boolean {
  if (!logic || !logic.conditions || logic.conditions.length === 0) {
    return true
  }

  const results = logic.conditions.map((c) => evaluateCondition(c, answers))

  const conditionsMet =
    logic.logicOperator === 'AND' ? results.every(Boolean) : results.some(Boolean)

  return logic.action === 'SHOW' ? conditionsMet : !conditionsMet
}

/**
 * Validate a single field value against its configuration.
 * Returns an error message string or null if valid.
 */
export function validateField(
  field: QuestionnaireField,
  value: unknown,
): string | null {
  // Required check
  if (field.required) {
    if (value === null || value === undefined || value === '') {
      return field.validationRules?.customMessage ?? `${field.label}为必填项`
    }
    if (Array.isArray(value) && value.length === 0) {
      return field.validationRules?.customMessage ?? `${field.label}为必填项`
    }
  }

  // Skip further validation if empty and not required
  if (value === null || value === undefined || value === '') return null
  if (Array.isArray(value) && value.length === 0) return null

  const rules = field.validationRules
  if (!rules) return null

  const custom = rules.customMessage

  // TEXT validations
  if (field.type === 'TEXT') {
    const str = String(value)
    if (rules.minLength !== undefined && str.length < rules.minLength) {
      return custom ?? `${field.label}长度不能少于${rules.minLength}个字符`
    }
    if (rules.maxLength !== undefined && str.length > rules.maxLength) {
      return custom ?? `${field.label}长度不能超过${rules.maxLength}个字符`
    }
    if (rules.pattern) {
      const regex = new RegExp(rules.pattern)
      if (!regex.test(str)) {
        return custom ?? `${field.label}格式不正确`
      }
    }
  }

  // NUMBER validations
  if (field.type === 'NUMBER') {
    const num = Number(value)
    if (rules.min !== undefined && num < rules.min) {
      return custom ?? `${field.label}不能小于${rules.min}`
    }
    if (rules.max !== undefined && num > rules.max) {
      return custom ?? `${field.label}不能大于${rules.max}`
    }
  }

  // DATE validations
  if (field.type === 'DATE') {
    const dateStr = String(value)
    if (rules.minDate && dateStr < rules.minDate) {
      return custom ?? `${field.label}不能早于${rules.minDate}`
    }
    if (rules.maxDate && dateStr > rules.maxDate) {
      return custom ?? `${field.label}不能晚于${rules.maxDate}`
    }
  }

  // MULTI_CHOICE validations
  if (field.type === 'MULTI_CHOICE' && Array.isArray(value)) {
    if (rules.minSelect !== undefined && value.length < rules.minSelect) {
      return custom ?? `${field.label}至少选择${rules.minSelect}项`
    }
    if (rules.maxSelect !== undefined && value.length > rules.maxSelect) {
      return custom ?? `${field.label}最多选择${rules.maxSelect}项`
    }
  }

  return null
}
