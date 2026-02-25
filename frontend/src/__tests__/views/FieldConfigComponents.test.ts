import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import ValidationRuleEditor from '@/components/questionnaire/ValidationRuleEditor.vue'
import ConditionalLogicEditor from '@/components/questionnaire/ConditionalLogicEditor.vue'
import GroupManager from '@/components/questionnaire/GroupManager.vue'
import FieldConfigPanel from '@/components/questionnaire/FieldConfigPanel.vue'
import { createDefaultField } from '@/types/questionnaire'
import type { QuestionnaireField, ValidationRules, ConditionalLogic, FieldGroup } from '@/types/questionnaire'

// Mock vuedraggable
vi.mock('vuedraggable', () => {
  const { defineComponent, h } = require('vue')
  return {
    default: defineComponent({
      name: 'draggable',
      props: ['list', 'group', 'itemKey', 'sort', 'clone', 'ghostClass', 'modelValue'],
      emits: ['update:modelValue', 'change'],
      setup(props: any, { slots }: any) {
        return () => {
          const items = props.list || props.modelValue || []
          const children = items.map((el: any, idx: number) => {
            if (slots.item) return slots.item({ element: el, index: idx })
            return null
          })
          return h('div', { class: 'mock-draggable' }, children)
        }
      },
    }),
  }
})

function mountWithPlugins(component: any, props: Record<string, any> = {}) {
  return mount(component, {
    props,
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('ValidationRuleEditor', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('should show TEXT fields: minLength, maxLength, pattern', () => {
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: null,
      fieldType: 'TEXT',
    })
    expect(wrapper.text()).toContain('最小长度')
    expect(wrapper.text()).toContain('最大长度')
    expect(wrapper.text()).toContain('正则表达式')
    expect(wrapper.text()).toContain('自定义错误提示')
  })

  it('should show NUMBER fields: min, max', () => {
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: null,
      fieldType: 'NUMBER',
    })
    expect(wrapper.text()).toContain('最小值')
    expect(wrapper.text()).toContain('最大值')
    expect(wrapper.text()).not.toContain('最小长度')
  })

  it('should show DATE fields: minDate, maxDate', () => {
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: null,
      fieldType: 'DATE',
    })
    expect(wrapper.text()).toContain('最早日期')
    expect(wrapper.text()).toContain('最晚日期')
  })

  it('should show MULTI_CHOICE fields: minSelect, maxSelect', () => {
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: null,
      fieldType: 'MULTI_CHOICE',
    })
    expect(wrapper.text()).toContain('最少选择')
    expect(wrapper.text()).toContain('最多选择')
  })

  it('should not show irrelevant fields for SINGLE_CHOICE', () => {
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: null,
      fieldType: 'SINGLE_CHOICE',
    })
    expect(wrapper.text()).not.toContain('最小长度')
    expect(wrapper.text()).not.toContain('最小值')
    expect(wrapper.text()).not.toContain('最早日期')
    expect(wrapper.text()).not.toContain('最少选择')
    // Custom message always shown
    expect(wrapper.text()).toContain('自定义错误提示')
  })

  it('should initialize with existing rules', () => {
    const rules: ValidationRules = { minLength: 5, maxLength: 100 }
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: rules,
      fieldType: 'TEXT',
    })
    expect(wrapper.text()).toContain('验证规则')
  })

  it('should display section title', () => {
    const wrapper = mountWithPlugins(ValidationRuleEditor, {
      modelValue: null,
      fieldType: 'TEXT',
    })
    expect(wrapper.text()).toContain('验证规则')
  })
})

describe('ConditionalLogicEditor', () => {
  beforeEach(() => setActivePinia(createPinia()))

  const sampleFields: QuestionnaireField[] = [
    createDefaultField('TEXT', 1),
    createDefaultField('SINGLE_CHOICE', 2),
  ]

  it('should show add button when no logic configured', () => {
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: null,
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('添加条件逻辑')
  })

  it('should show logic config when logic exists', () => {
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'AND',
      conditions: [{ fieldKey: 'field1', operator: 'EQUALS', value: 'test' }],
    }
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: logic,
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('动作')
    expect(wrapper.text()).toContain('逻辑运算')
    expect(wrapper.text()).toContain('添加条件')
    expect(wrapper.text()).toContain('清除逻辑')
  })

  it('should emit null when enable logic button is clicked then cleared', async () => {
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: null,
      availableFields: sampleFields,
    })
    // Click "添加条件逻辑"
    await wrapper.find('.no-logic .el-button').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    const firstEmit = wrapper.emitted('update:modelValue')![0][0] as ConditionalLogic
    expect(firstEmit.action).toBe('SHOW')
    expect(firstEmit.conditions.length).toBe(1)
  })

  it('should emit null when logic is cleared', async () => {
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'AND',
      conditions: [{ fieldKey: '', operator: 'EQUALS', value: '' }],
    }
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: logic,
      availableFields: sampleFields,
    })
    // Find and click "清除逻辑" button
    const buttons = wrapper.findAll('.logic-actions .el-button')
    const clearBtn = buttons.find((b) => b.text().includes('清除逻辑'))
    expect(clearBtn).toBeTruthy()
    await clearBtn!.trigger('click')
    const emits = wrapper.emitted('update:modelValue')!
    expect(emits[emits.length - 1][0]).toBeNull()
  })

  it('should display section title', () => {
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: null,
      availableFields: [],
    })
    expect(wrapper.text()).toContain('条件逻辑')
  })

  it('should show SHOW/HIDE radio options', () => {
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'AND',
      conditions: [{ fieldKey: '', operator: 'EQUALS', value: '' }],
    }
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: logic,
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('显示')
    expect(wrapper.text()).toContain('隐藏')
  })

  it('should show AND/OR radio options', () => {
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'AND',
      conditions: [{ fieldKey: '', operator: 'EQUALS', value: '' }],
    }
    const wrapper = mountWithPlugins(ConditionalLogicEditor, {
      modelValue: logic,
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('全部满足 (AND)')
    expect(wrapper.text()).toContain('任一满足 (OR)')
  })
})

describe('GroupManager', () => {
  beforeEach(() => setActivePinia(createPinia()))

  const sampleFields: QuestionnaireField[] = [
    createDefaultField('TEXT', 1),
    createDefaultField('NUMBER', 2),
  ]

  it('should show empty state when no groups', () => {
    const wrapper = mountWithPlugins(GroupManager, {
      modelValue: [],
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('暂无分组')
  })

  it('should show new group button', () => {
    const wrapper = mountWithPlugins(GroupManager, {
      modelValue: [],
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('新建分组')
  })

  it('should render existing groups', () => {
    const groups: FieldGroup[] = [
      { name: '基本信息', sortOrder: 1, fields: [] },
    ]
    const wrapper = mountWithPlugins(GroupManager, {
      modelValue: groups,
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('基本信息')
    expect(wrapper.text()).toContain('0 个字段')
  })

  it('should show field count for groups with fields', () => {
    const groups: FieldGroup[] = [
      { name: '测试组', sortOrder: 1, fields: ['field1', 'field2'] },
    ]
    const wrapper = mountWithPlugins(GroupManager, {
      modelValue: groups,
      availableFields: sampleFields,
    })
    expect(wrapper.text()).toContain('2 个字段')
  })

  it('should emit update when group is removed', async () => {
    const groups: FieldGroup[] = [
      { name: '待删除', sortOrder: 1, fields: [] },
    ]
    const wrapper = mountWithPlugins(GroupManager, {
      modelValue: groups,
      availableFields: sampleFields,
    })
    // Click delete button
    const deleteBtn = wrapper.find('.group-header .el-button')
    await deleteBtn.trigger('click')
    const emits = wrapper.emitted('update:modelValue')!
    expect(emits[emits.length - 1][0]).toEqual([])
  })

  it('should display section title', () => {
    const wrapper = mountWithPlugins(GroupManager, {
      modelValue: [],
      availableFields: [],
    })
    expect(wrapper.text()).toContain('分组管理')
  })
})

describe('FieldConfigPanel integration', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('should render ValidationRuleEditor when field is selected', () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field, allFields: [field] })
    expect(wrapper.text()).toContain('验证规则')
  })

  it('should render ConditionalLogicEditor when field is selected', () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field, allFields: [field] })
    expect(wrapper.text()).toContain('条件逻辑')
  })

  it('should show validation fields appropriate for TEXT type', () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field, allFields: [field] })
    expect(wrapper.text()).toContain('最小长度')
    expect(wrapper.text()).toContain('最大长度')
    expect(wrapper.text()).toContain('正则表达式')
  })

  it('should show validation fields appropriate for NUMBER type', () => {
    const field = createDefaultField('NUMBER', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field, allFields: [field] })
    expect(wrapper.text()).toContain('最小值')
    expect(wrapper.text()).toContain('最大值')
  })

  it('should still show placeholder when no field selected', () => {
    const wrapper = mountWithPlugins(FieldConfigPanel, { field: null })
    expect(wrapper.text()).toContain('请在画布中选择一个字段')
    expect(wrapper.text()).not.toContain('验证规则')
  })

  it('should emit update when validation rules change', async () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field, allFields: [field] })
    const vrEditor = wrapper.findComponent(ValidationRuleEditor)
    vrEditor.vm.$emit('update:modelValue', { minLength: 5 })
    await wrapper.vm.$nextTick()
    const emits = wrapper.emitted('update')
    expect(emits).toBeTruthy()
    const lastEmit = emits![emits!.length - 1][0] as QuestionnaireField
    expect(lastEmit.validationRules).toEqual({ minLength: 5 })
  })

  it('should emit update when conditional logic changes', async () => {
    const field = createDefaultField('TEXT', 1)
    const otherField = createDefaultField('SINGLE_CHOICE', 2)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field, allFields: [field, otherField] })
    const clEditor = wrapper.findComponent(ConditionalLogicEditor)
    const logic: ConditionalLogic = {
      action: 'SHOW',
      logicOperator: 'AND',
      conditions: [{ fieldKey: otherField.key, operator: 'EQUALS', value: 'yes' }],
    }
    clEditor.vm.$emit('update:modelValue', logic)
    await wrapper.vm.$nextTick()
    const emits = wrapper.emitted('update')
    expect(emits).toBeTruthy()
    const lastEmit = emits![emits!.length - 1][0] as QuestionnaireField
    expect(lastEmit.conditionalLogic).toEqual(logic)
  })
})
