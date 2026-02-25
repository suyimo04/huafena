import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import FieldRenderer from '@/components/questionnaire/FieldRenderer.vue'
import GroupRenderer from '@/components/questionnaire/GroupRenderer.vue'
import QuestionnaireRenderer from '@/components/questionnaire/QuestionnaireRenderer.vue'
import {
  evaluateCondition,
  evaluateConditionalLogic,
  validateField,
} from '@/utils/questionnaireLogic'
import type {
  QuestionnaireField,
  QuestionnaireSchema,
  ConditionalLogic,
} from '@/types/questionnaire'

function mountWithPlugins(component: any, props: Record<string, any> = {}) {
  return mount(component, {
    props,
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

// --- Utility function tests ---

describe('evaluateCondition', () => {
  it('EQUALS returns true when values match', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'EQUALS', value: 'yes' }, { a: 'yes' })).toBe(true)
  })

  it('EQUALS returns false when values differ', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'EQUALS', value: 'yes' }, { a: 'no' })).toBe(false)
  })

  it('NOT_EQUALS returns true when values differ', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'NOT_EQUALS', value: 'yes' }, { a: 'no' })).toBe(true)
  })

  it('CONTAINS returns true when string contains value', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'CONTAINS', value: 'ell' }, { a: 'hello' })).toBe(true)
  })

  it('CONTAINS returns true when array contains value', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'CONTAINS', value: 'x' }, { a: ['x', 'y'] })).toBe(true)
  })

  it('GREATER_THAN compares numerically', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'GREATER_THAN', value: '5' }, { a: 10 })).toBe(true)
    expect(evaluateCondition({ fieldKey: 'a', operator: 'GREATER_THAN', value: '5' }, { a: 3 })).toBe(false)
  })

  it('LESS_THAN compares numerically', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'LESS_THAN', value: '5' }, { a: 3 })).toBe(true)
  })

  it('IN returns true when value is in array', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'IN', value: ['x', 'y'] }, { a: 'x' })).toBe(true)
    expect(evaluateCondition({ fieldKey: 'a', operator: 'IN', value: ['x', 'y'] }, { a: 'z' })).toBe(false)
  })

  it('NOT_IN returns true when value is not in array', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'NOT_IN', value: ['x', 'y'] }, { a: 'z' })).toBe(true)
    expect(evaluateCondition({ fieldKey: 'a', operator: 'NOT_IN', value: ['x', 'y'] }, { a: 'x' })).toBe(false)
  })

  it('handles missing field value gracefully', () => {
    expect(evaluateCondition({ fieldKey: 'a', operator: 'EQUALS', value: '' }, {})).toBe(true)
    expect(evaluateCondition({ fieldKey: 'a', operator: 'EQUALS', value: 'yes' }, {})).toBe(false)
  })
})

describe('evaluateConditionalLogic', () => {
  it('returns true when logic is null', () => {
    expect(evaluateConditionalLogic(null, {})).toBe(true)
  })

  it('returns true when conditions array is empty', () => {
    const logic: ConditionalLogic = { action: 'SHOW', logicOperator: 'AND', conditions: [] }
    expect(evaluateConditionalLogic(logic, {})).toBe(true)
  })

  it('SHOW + AND: visible when all conditions met', () => {
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'AND',
      conditions: [
        { fieldKey: 'a', operator: 'EQUALS', value: '1' },
        { fieldKey: 'b', operator: 'EQUALS', value: '2' },
      ],
    }
    expect(evaluateConditionalLogic(logic, { a: '1', b: '2' })).toBe(true)
    expect(evaluateConditionalLogic(logic, { a: '1', b: '3' })).toBe(false)
  })

  it('SHOW + OR: visible when any condition met', () => {
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'OR',
      conditions: [
        { fieldKey: 'a', operator: 'EQUALS', value: '1' },
        { fieldKey: 'b', operator: 'EQUALS', value: '2' },
      ],
    }
    expect(evaluateConditionalLogic(logic, { a: '1', b: '3' })).toBe(true)
    expect(evaluateConditionalLogic(logic, { a: '0', b: '0' })).toBe(false)
  })

  it('HIDE + AND: hidden when all conditions met', () => {
    const logic: ConditionalLogic = {
      action: 'HIDE',
      logicOperator: 'AND',
      conditions: [{ fieldKey: 'a', operator: 'EQUALS', value: '1' }],
    }
    expect(evaluateConditionalLogic(logic, { a: '1' })).toBe(false)
    expect(evaluateConditionalLogic(logic, { a: '2' })).toBe(true)
  })
})

describe('validateField', () => {
  function makeField(overrides: Partial<QuestionnaireField> = {}): QuestionnaireField {
    return {
      key: 'test',
      type: 'TEXT',
      label: '测试',
      required: false,
      validationRules: null,
      conditionalLogic: null,
      options: null,
      ...overrides,
    }
  }

  it('returns error for empty required field', () => {
    const field = makeField({ required: true })
    expect(validateField(field, '')).toContain('必填')
  })

  it('returns null for empty non-required field', () => {
    expect(validateField(makeField(), '')).toBeNull()
  })

  it('validates minLength for TEXT', () => {
    const field = makeField({ validationRules: { minLength: 3 } })
    expect(validateField(field, 'ab')).toContain('不能少于')
    expect(validateField(field, 'abc')).toBeNull()
  })

  it('validates maxLength for TEXT', () => {
    const field = makeField({ validationRules: { maxLength: 3 } })
    expect(validateField(field, 'abcd')).toContain('不能超过')
    expect(validateField(field, 'abc')).toBeNull()
  })

  it('validates pattern for TEXT', () => {
    const field = makeField({ validationRules: { pattern: '^\\d+$' } })
    expect(validateField(field, 'abc')).toContain('格式不正确')
    expect(validateField(field, '123')).toBeNull()
  })

  it('validates min/max for NUMBER', () => {
    const field = makeField({ type: 'NUMBER', validationRules: { min: 1, max: 10 } })
    expect(validateField(field, 0)).toContain('不能小于')
    expect(validateField(field, 11)).toContain('不能大于')
    expect(validateField(field, 5)).toBeNull()
  })

  it('validates minDate/maxDate for DATE', () => {
    const field = makeField({ type: 'DATE', validationRules: { minDate: '2024-01-01', maxDate: '2024-12-31' } })
    expect(validateField(field, '2023-12-31')).toContain('不能早于')
    expect(validateField(field, '2025-01-01')).toContain('不能晚于')
    expect(validateField(field, '2024-06-15')).toBeNull()
  })

  it('validates minSelect/maxSelect for MULTI_CHOICE', () => {
    const field = makeField({ type: 'MULTI_CHOICE', validationRules: { minSelect: 2, maxSelect: 3 } })
    expect(validateField(field, ['a'])).toContain('至少选择')
    expect(validateField(field, ['a', 'b', 'c', 'd'])).toContain('最多选择')
    expect(validateField(field, ['a', 'b'])).toBeNull()
  })

  it('uses customMessage when provided', () => {
    const field = makeField({ required: true, validationRules: { customMessage: '自定义错误' } })
    expect(validateField(field, '')).toBe('自定义错误')
  })

  it('returns null for required field with empty array (MULTI_CHOICE)', () => {
    const field = makeField({ type: 'MULTI_CHOICE', required: true })
    expect(validateField(field, [])).toContain('必填')
  })
})

// --- Component tests ---

describe('FieldRenderer', () => {
  beforeEach(() => setActivePinia(createPinia()))

  function makeField(overrides: Partial<QuestionnaireField> = {}): QuestionnaireField {
    return {
      key: 'f1', type: 'TEXT', label: '姓名', required: false,
      validationRules: null, conditionalLogic: null, options: null,
      ...overrides,
    }
  }

  it('renders label and required star', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField({ required: true }),
      modelValue: '',
      error: null,
    })
    expect(wrapper.text()).toContain('姓名')
    expect(wrapper.find('.field-required-star').exists()).toBe(true)
  })

  it('renders ElInput for TEXT type', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField(),
      modelValue: '',
      error: null,
    })
    expect(wrapper.findComponent({ name: 'ElInput' }).exists()).toBe(true)
  })

  it('renders ElInputNumber for NUMBER type', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField({ type: 'NUMBER' }),
      modelValue: 0,
      error: null,
    })
    expect(wrapper.findComponent({ name: 'ElInputNumber' }).exists()).toBe(true)
  })

  it('renders ElRadioGroup for SINGLE_CHOICE type', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField({ type: 'SINGLE_CHOICE', options: [{ value: 'a', label: 'A' }] }),
      modelValue: '',
      error: null,
    })
    expect(wrapper.findComponent({ name: 'ElRadioGroup' }).exists()).toBe(true)
  })

  it('renders ElCheckboxGroup for MULTI_CHOICE type', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField({ type: 'MULTI_CHOICE', options: [{ value: 'a', label: 'A' }] }),
      modelValue: [],
      error: null,
    })
    expect(wrapper.findComponent({ name: 'ElCheckboxGroup' }).exists()).toBe(true)
  })

  it('renders ElSelect for DROPDOWN type', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField({ type: 'DROPDOWN', options: [{ value: 'a', label: 'A' }] }),
      modelValue: '',
      error: null,
    })
    expect(wrapper.findComponent({ name: 'ElSelect' }).exists()).toBe(true)
  })

  it('renders ElDatePicker for DATE type', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField({ type: 'DATE' }),
      modelValue: '',
      error: null,
    })
    expect(wrapper.findComponent({ name: 'ElDatePicker' }).exists()).toBe(true)
  })

  it('shows error message when error prop is set', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField(),
      modelValue: '',
      error: '此字段必填',
    })
    expect(wrapper.find('.field-error').text()).toBe('此字段必填')
  })

  it('does not show error when error is null', () => {
    const wrapper = mountWithPlugins(FieldRenderer, {
      field: makeField(),
      modelValue: '',
      error: null,
    })
    expect(wrapper.find('.field-error').exists()).toBe(false)
  })
})

describe('GroupRenderer', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('renders group title', () => {
    const wrapper = mountWithPlugins(GroupRenderer, {
      group: { name: '基本信息', sortOrder: 1, fields: [] },
      fields: [],
      answers: {},
      errors: {},
    })
    expect(wrapper.find('.group-title').text()).toBe('基本信息')
  })

  it('renders visible fields in the group', () => {
    const fields: QuestionnaireField[] = [
      { key: 'f1', type: 'TEXT', label: '姓名', required: false, validationRules: null, conditionalLogic: null, options: null },
    ]
    const wrapper = mountWithPlugins(GroupRenderer, {
      group: { name: '基本信息', sortOrder: 1, fields: ['f1'] },
      fields,
      answers: {},
      errors: {},
    })
    expect(wrapper.findAllComponents(FieldRenderer).length).toBe(1)
  })

  it('hides fields when conditional logic evaluates to hidden', () => {
    const fields: QuestionnaireField[] = [
      {
        key: 'f1', type: 'TEXT', label: '学校', required: false,
        validationRules: null, options: null,
        conditionalLogic: {
          action: 'SHOW',
          logicOperator: 'AND',
          conditions: [{ fieldKey: 'student', operator: 'EQUALS', value: 'yes' }],
        },
      },
    ]
    const wrapper = mountWithPlugins(GroupRenderer, {
      group: { name: '教育', sortOrder: 1, fields: ['f1'] },
      fields,
      answers: { student: 'no' },
      errors: {},
    })
    expect(wrapper.findAllComponents(FieldRenderer).length).toBe(0)
  })
})

describe('QuestionnaireRenderer', () => {
  beforeEach(() => setActivePinia(createPinia()))

  const basicSchema: QuestionnaireSchema = {
    groups: [
      { name: '基本信息', sortOrder: 1, fields: ['name'] },
    ],
    fields: [
      { key: 'name', type: 'TEXT', label: '姓名', required: true, validationRules: { minLength: 2 }, conditionalLogic: null, options: null },
      { key: 'extra', type: 'TEXT', label: '备注', required: false, validationRules: null, conditionalLogic: null, options: null },
    ],
  }

  it('renders grouped and ungrouped fields', () => {
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema: basicSchema })
    expect(wrapper.findAllComponents(GroupRenderer).length).toBe(1)
    // Ungrouped 'extra' field rendered directly
    const allFieldRenderers = wrapper.findAllComponents(FieldRenderer)
    expect(allFieldRenderers.length).toBeGreaterThanOrEqual(2)
  })

  it('validate() returns errors for invalid fields', () => {
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema: basicSchema })
    const vm = wrapper.vm as any
    const errs = vm.validate()
    expect(errs).toHaveProperty('name')
  })

  it('validate() returns empty object when all valid', () => {
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema: basicSchema })
    const vm = wrapper.vm as any
    vm.answers = { name: '张三' }
    const errs = vm.validate()
    expect(Object.keys(errs).length).toBe(0)
  })

  it('getAnswers() returns current form data', () => {
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema: basicSchema })
    const vm = wrapper.vm as any
    vm.answers = { name: '李四', extra: '无' }
    const data = vm.getAnswers()
    expect(data).toEqual({ name: '李四', extra: '无' })
  })

  it('emits submit with answers when validation passes', async () => {
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema: basicSchema })
    const vm = wrapper.vm as any
    vm.answers = { name: '王五' }
    await wrapper.find('.el-button--primary').trigger('click')
    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')![0][0]).toEqual({ name: '王五' })
  })

  it('does not emit submit when validation fails', async () => {
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema: basicSchema })
    await wrapper.find('.el-button--primary').trigger('click')
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('skips validation for hidden fields', () => {
    const schema: QuestionnaireSchema = {
      groups: [],
      fields: [
        {
          key: 'hidden_field', type: 'TEXT', label: '隐藏', required: true,
          validationRules: null, options: null,
          conditionalLogic: {
            action: 'SHOW',
            logicOperator: 'AND',
            conditions: [{ fieldKey: 'toggle', operator: 'EQUALS', value: 'yes' }],
          },
        },
      ],
    }
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema })
    const vm = wrapper.vm as any
    vm.answers = { toggle: 'no' }
    const errs = vm.validate()
    expect(Object.keys(errs).length).toBe(0)
  })

  it('sorts groups by sortOrder', () => {
    const schema: QuestionnaireSchema = {
      groups: [
        { name: '第二组', sortOrder: 2, fields: [] },
        { name: '第一组', sortOrder: 1, fields: [] },
      ],
      fields: [],
    }
    const wrapper = mountWithPlugins(QuestionnaireRenderer, { schema })
    const titles = wrapper.findAll('.group-title')
    expect(titles[0].text()).toBe('第一组')
    expect(titles[1].text()).toBe('第二组')
  })
})
