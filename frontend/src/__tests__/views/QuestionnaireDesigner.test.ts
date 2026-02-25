import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import QuestionnaireDesigner from '@/components/questionnaire/QuestionnaireDesigner.vue'
import FieldPalette from '@/components/questionnaire/FieldPalette.vue'
import DesignCanvas from '@/components/questionnaire/DesignCanvas.vue'
import FieldConfigPanel from '@/components/questionnaire/FieldConfigPanel.vue'
import { FIELD_TYPE_LIST, createDefaultField } from '@/types/questionnaire'
import type { QuestionnaireField } from '@/types/questionnaire'

// Mock vuedraggable with a functional render component
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
            if (slots.item) {
              return slots.item({ element: el, index: idx })
            }
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

describe('QuestionnaireDesigner', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render three-panel layout', () => {
    const wrapper = mountWithPlugins(QuestionnaireDesigner)
    expect(wrapper.find('.panel-left').exists()).toBe(true)
    expect(wrapper.find('.panel-center').exists()).toBe(true)
    expect(wrapper.find('.panel-right').exists()).toBe(true)
  })

  it('should contain FieldPalette, DesignCanvas, and FieldConfigPanel', () => {
    const wrapper = mountWithPlugins(QuestionnaireDesigner)
    expect(wrapper.findComponent(FieldPalette).exists()).toBe(true)
    expect(wrapper.findComponent(DesignCanvas).exists()).toBe(true)
    expect(wrapper.findComponent(FieldConfigPanel).exists()).toBe(true)
  })

  it('should start with empty fields and no selection', () => {
    const wrapper = mountWithPlugins(QuestionnaireDesigner)
    const vm = wrapper.vm as any
    expect(vm.fields).toEqual([])
    expect(vm.selectedFieldKey).toBeNull()
  })

  it('should remove a field and clear selection if it was selected', async () => {
    const wrapper = mountWithPlugins(QuestionnaireDesigner)
    const vm = wrapper.vm as any

    const field = createDefaultField('TEXT', 1)
    vm.fields.push(field)
    vm.selectedFieldKey = field.key
    await wrapper.vm.$nextTick()

    // Trigger remove via DesignCanvas emit
    wrapper.findComponent(DesignCanvas).vm.$emit('remove', 0)
    await wrapper.vm.$nextTick()

    expect(vm.fields.length).toBe(0)
    expect(vm.selectedFieldKey).toBeNull()
  })

  it('should update field config when FieldConfigPanel emits update', async () => {
    const wrapper = mountWithPlugins(QuestionnaireDesigner)
    const vm = wrapper.vm as any

    const field = createDefaultField('TEXT', 1)
    vm.fields.push(field)
    vm.selectedFieldKey = field.key
    await wrapper.vm.$nextTick()

    const updated = { ...field, label: '新标签' }
    wrapper.findComponent(FieldConfigPanel).vm.$emit('update', updated)
    await wrapper.vm.$nextTick()

    expect(vm.fields[0].label).toBe('新标签')
  })
})

describe('FieldPalette', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render all field types', () => {
    const wrapper = mountWithPlugins(FieldPalette)
    for (const item of FIELD_TYPE_LIST) {
      expect(wrapper.text()).toContain(item.label)
    }
  })

  it('should render 6 palette items', () => {
    const wrapper = mountWithPlugins(FieldPalette)
    const items = wrapper.findAll('.palette-item')
    expect(items.length).toBe(6)
  })

  it('should display title and hint text', () => {
    const wrapper = mountWithPlugins(FieldPalette)
    expect(wrapper.text()).toContain('字段类型')
    expect(wrapper.text()).toContain('拖拽字段到画布中')
  })
})

describe('DesignCanvas', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should show empty hint when no fields', () => {
    const wrapper = mountWithPlugins(DesignCanvas, { fields: [], selectedKey: null })
    expect(wrapper.text()).toContain('将左侧字段拖拽到此处')
  })

  it('should render fields passed as props', () => {
    const fields: QuestionnaireField[] = [
      createDefaultField('TEXT', 1),
      createDefaultField('NUMBER', 2),
    ]
    const wrapper = mountWithPlugins(DesignCanvas, { fields, selectedKey: null })
    expect(wrapper.findAll('.canvas-field').length).toBe(2)
  })

  it('should display field count', () => {
    const fields = [createDefaultField('TEXT', 1)]
    const wrapper = mountWithPlugins(DesignCanvas, { fields, selectedKey: null })
    expect(wrapper.text()).toContain('1 个字段')
  })

  it('should highlight selected field', () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(DesignCanvas, { fields: [field], selectedKey: field.key })
    expect(wrapper.find('.canvas-field-active').exists()).toBe(true)
  })

  it('should emit select when a field is clicked', async () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(DesignCanvas, { fields: [field], selectedKey: null })
    await wrapper.find('.canvas-field').trigger('click')
    expect(wrapper.emitted('select')?.[0]).toEqual([field.key])
  })

  it('should emit remove when delete button is clicked', async () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(DesignCanvas, { fields: [field], selectedKey: null })
    await wrapper.find('.field-actions .el-button').trigger('click')
    expect(wrapper.emitted('remove')?.[0]).toEqual([0])
  })

  it('should show required indicator for required fields', () => {
    const field = createDefaultField('TEXT', 1)
    field.required = true
    const wrapper = mountWithPlugins(DesignCanvas, { fields: [field], selectedKey: null })
    expect(wrapper.find('.field-required').exists()).toBe(true)
  })
})

describe('FieldConfigPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should show placeholder when no field is selected', () => {
    const wrapper = mountWithPlugins(FieldConfigPanel, { field: null })
    expect(wrapper.text()).toContain('请在画布中选择一个字段')
  })

  it('should render config form when a field is selected', () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field })
    expect(wrapper.text()).toContain('字段属性')
    expect(wrapper.text()).toContain('字段标识')
    expect(wrapper.text()).toContain('字段标签')
    expect(wrapper.text()).toContain('字段类型')
    expect(wrapper.text()).toContain('是否必填')
  })

  it('should show options editor for choice-type fields', () => {
    const field = createDefaultField('SINGLE_CHOICE', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field })
    expect(wrapper.text()).toContain('选项列表')
    expect(wrapper.text()).toContain('添加选项')
  })

  it('should not show options editor for text fields', () => {
    const field = createDefaultField('TEXT', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field })
    expect(wrapper.text()).not.toContain('选项列表')
  })

  it('should show options editor for MULTI_CHOICE fields', () => {
    const field = createDefaultField('MULTI_CHOICE', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field })
    expect(wrapper.text()).toContain('选项列表')
  })

  it('should show options editor for DROPDOWN fields', () => {
    const field = createDefaultField('DROPDOWN', 1)
    const wrapper = mountWithPlugins(FieldConfigPanel, { field })
    expect(wrapper.text()).toContain('选项列表')
  })
})

describe('createDefaultField', () => {
  it('should create a TEXT field without options', () => {
    const field = createDefaultField('TEXT', 1)
    expect(field.type).toBe('TEXT')
    expect(field.options).toBeNull()
    expect(field.required).toBe(false)
    expect(field.key).toContain('field_')
  })

  it('should create a SINGLE_CHOICE field with default options', () => {
    const field = createDefaultField('SINGLE_CHOICE', 1)
    expect(field.type).toBe('SINGLE_CHOICE')
    expect(field.options).toHaveLength(2)
    expect(field.options![0].label).toBe('选项 1')
  })

  it('should create a DROPDOWN field with default options', () => {
    const field = createDefaultField('DROPDOWN', 1)
    expect(field.options).toHaveLength(2)
  })

  it('should create unique keys for different fields', () => {
    const f1 = createDefaultField('TEXT', 1)
    const f2 = createDefaultField('TEXT', 2)
    expect(f1.key).not.toBe(f2.key)
  })
})
