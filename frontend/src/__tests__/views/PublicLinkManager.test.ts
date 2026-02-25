import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import PublicLinkManager from '@/components/application/PublicLinkManager.vue'

vi.mock('@/api/application', () => ({
  getPublicLinks: vi.fn(),
  generatePublicLink: vi.fn(),
}))

vi.mock('@/api/questionnaire', () => ({
  getTemplates: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  }
})

const mockLinks = [
  {
    id: 1,
    linkToken: 'abc-123-def',
    templateId: 10,
    versionId: 20,
    active: true,
    createdAt: '2024-03-01T10:00:00',
    expiresAt: null,
  },
  {
    id: 2,
    linkToken: 'xyz-456-ghi',
    templateId: 11,
    versionId: 21,
    active: false,
    createdAt: '2024-02-15T08:00:00',
    expiresAt: '2024-04-01T00:00:00',
  },
]

const mockTemplates = [
  { id: 10, title: '招募问卷A', description: '', activeVersionId: 20, createdBy: 1, createdAt: '', updatedAt: '' },
  { id: 11, title: '招募问卷B', description: '', activeVersionId: 21, createdBy: 1, createdAt: '', updatedAt: '' },
]

function createWrapper() {
  return mount(PublicLinkManager, {
    global: {
      plugins: [createPinia(), ElementPlus],
      stubs: {
        ElDialog: {
          template: '<div class="el-dialog" v-if="modelValue"><slot /><slot name="footer" /></div>',
          props: ['modelValue'],
        },
      },
    },
  })
}

describe('PublicLinkManager', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render title and generate button', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('公开问卷链接')
    expect(wrapper.text()).toContain('生成公开链接')
  })

  it('should fetch and display public links on mount', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockLinks })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })

    const wrapper = createWrapper()
    await flushPromises()

    expect(appApi.getPublicLinks).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('abc-123-def')
    expect(wrapper.text()).toContain('xyz-456-ghi')
  })

  it('should display active/inactive status tags', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockLinks })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('有效')
    expect(wrapper.text()).toContain('已失效')
  })

  it('should display expiry info correctly', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockLinks })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('永不过期')
    expect(wrapper.text()).toContain('2024-04-01 00:00:00')
  })

  it('should show copy link button for each row', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockLinks })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })

    const wrapper = createWrapper()
    await flushPromises()

    const copyButtons = wrapper.findAll('.el-button').filter((b) => b.text() === '复制链接')
    expect(copyButtons.length).toBe(2)
  })

  it('should open generate dialog when clicking generate button', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })

    const wrapper = createWrapper()
    await flushPromises()

    const genBtn = wrapper.findAll('.el-button').find((b) => b.text() === '生成公开链接')
    expect(genBtn).toBeDefined()
    await genBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('生成公开链接')
    // Dialog should now be visible with template options
    expect(wrapper.find('.el-dialog').exists()).toBe(true)
  })

  it('should call generatePublicLink API and refresh list', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: [mockLinks[0]] })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })
    ;(appApi.generatePublicLink as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockLinks[0] })

    const wrapper = createWrapper()
    await flushPromises()

    // Open dialog
    const genBtn = wrapper.findAll('.el-button').find((b) => b.text() === '生成公开链接')
    await genBtn!.trigger('click')
    await flushPromises()

    // Set template selection via component state
    const vm = wrapper.vm as any
    vm.selectedTemplateId = 10
    await flushPromises()

    // Click generate in dialog
    const dialogGenBtn = wrapper.findAll('.el-button').find((b) => b.text() === '生成')
    expect(dialogGenBtn).toBeDefined()
    await dialogGenBtn!.trigger('click')
    await flushPromises()

    expect(appApi.generatePublicLink).toHaveBeenCalledWith({ templateId: 10 })
    // Should refresh links
    expect(appApi.getPublicLinks).toHaveBeenCalledTimes(2)
  })

  it('should handle empty link list', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.find('.el-table').exists()).toBe(true)
    expect(appApi.getPublicLinks).toHaveBeenCalledOnce()
  })

  it('should copy link to clipboard when clicking copy button', async () => {
    const appApi = await import('@/api/application')
    const qApi = await import('@/api/questionnaire')
    ;(appApi.getPublicLinks as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [mockLinks[0]] })
    ;(qApi.getTemplates as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockTemplates })

    // Mock clipboard
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })

    const wrapper = createWrapper()
    await flushPromises()

    const copyBtn = wrapper.findAll('.el-button').find((b) => b.text() === '复制链接')
    await copyBtn!.trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(
      expect.stringContaining('/public/questionnaire/abc-123-def'),
    )
  })
})
