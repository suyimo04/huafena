import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import VersionHistory from '@/components/questionnaire/VersionHistory.vue'
import type { VersionDTO } from '@/api/questionnaire'

// Mock vuedraggable (needed by parent components in some test setups)
vi.mock('vuedraggable', () => {
  const { defineComponent, h } = require('vue')
  return {
    default: defineComponent({
      name: 'draggable',
      props: ['list', 'group', 'itemKey', 'sort', 'clone', 'ghostClass', 'modelValue'],
      emits: ['update:modelValue', 'change'],
      setup(props: any, { slots }: any) {
        return () => h('div', { class: 'mock-draggable' })
      },
    }),
  }
})

function createVersion(overrides: Partial<VersionDTO> = {}): VersionDTO {
  return {
    id: 1,
    templateId: 100,
    versionNumber: 1,
    schemaDefinition: { groups: [], fields: [] },
    status: 'DRAFT',
    createdAt: '2025-01-15T10:30:00',
    ...overrides,
  }
}

function mountComponent(props: Record<string, any> = {}) {
  return mount(VersionHistory, {
    props: {
      versions: [],
      activeVersionId: null,
      ...props,
    },
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('VersionHistory', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render title and action buttons', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('版本历史')
    expect(wrapper.text()).toContain('保存')
    expect(wrapper.text()).toContain('发布')
  })

  it('should show empty message when no versions', () => {
    const wrapper = mountComponent({ versions: [] })
    expect(wrapper.text()).toContain('暂无版本记录')
  })

  it('should render version list', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'PUBLISHED' }),
      createVersion({ id: 2, versionNumber: 2, status: 'DRAFT' }),
    ]
    const wrapper = mountComponent({ versions })
    expect(wrapper.findAll('.version-item').length).toBe(2)
    expect(wrapper.text()).toContain('v1')
    expect(wrapper.text()).toContain('v2')
  })

  it('should display status tags correctly', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'PUBLISHED' }),
      createVersion({ id: 2, versionNumber: 2, status: 'DRAFT' }),
    ]
    const wrapper = mountComponent({ versions })
    expect(wrapper.text()).toContain('已发布')
    expect(wrapper.text()).toContain('草稿')
  })

  it('should highlight active version with "当前" tag', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'PUBLISHED' }),
    ]
    const wrapper = mountComponent({ versions, activeVersionId: 1 })
    expect(wrapper.text()).toContain('当前')
    expect(wrapper.find('.version-active').exists()).toBe(true)
  })

  it('should not show "当前" tag for non-active versions', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'DRAFT' }),
    ]
    const wrapper = mountComponent({ versions, activeVersionId: 999 })
    expect(wrapper.text()).not.toContain('当前')
  })

  it('should emit save when save button is clicked', async () => {
    const wrapper = mountComponent()
    const buttons = wrapper.findAll('.el-button')
    const saveBtn = buttons.find((b) => b.text().includes('保存'))
    await saveBtn!.trigger('click')
    expect(wrapper.emitted('save')).toHaveLength(1)
  })

  it('should emit publish when publish button is clicked', async () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'DRAFT' }),
    ]
    const wrapper = mountComponent({ versions })
    const buttons = wrapper.findAll('.el-button')
    const publishBtn = buttons.find((b) => b.text().includes('发布'))
    await publishBtn!.trigger('click')
    expect(wrapper.emitted('publish')).toHaveLength(1)
  })

  it('should disable publish button when latest version is PUBLISHED', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'PUBLISHED' }),
    ]
    const wrapper = mountComponent({ versions })
    const buttons = wrapper.findAll('.el-button')
    const publishBtn = buttons.find((b) => b.text().includes('发布'))
    expect(publishBtn!.attributes('disabled')).toBeDefined()
  })

  it('should enable publish button when latest version is DRAFT', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, status: 'DRAFT' }),
    ]
    const wrapper = mountComponent({ versions })
    const buttons = wrapper.findAll('.el-button')
    const publishBtn = buttons.find((b) => b.text().includes('发布'))
    expect(publishBtn!.attributes('disabled')).toBeUndefined()
  })

  it('should emit selectVersion when a version item is clicked', async () => {
    const ver = createVersion({ id: 1, versionNumber: 1 })
    const wrapper = mountComponent({ versions: [ver] })
    await wrapper.find('.version-item').trigger('click')
    expect(wrapper.emitted('selectVersion')).toHaveLength(1)
    expect(wrapper.emitted('selectVersion')![0]).toEqual([ver])
  })

  it('should mark clicked version as selected', async () => {
    const ver = createVersion({ id: 1, versionNumber: 1 })
    const wrapper = mountComponent({ versions: [ver] })
    await wrapper.find('.version-item').trigger('click')
    expect(wrapper.find('.version-selected').exists()).toBe(true)
  })

  it('should format date correctly', () => {
    const versions: VersionDTO[] = [
      createVersion({ id: 1, versionNumber: 1, createdAt: '2025-06-15T08:05:00' }),
    ]
    const wrapper = mountComponent({ versions })
    expect(wrapper.text()).toContain('2025-06-15 08:05')
  })

  it('should show loading state', () => {
    const wrapper = mountComponent({ loading: true })
    expect(wrapper.text()).toContain('加载中')
  })
})
