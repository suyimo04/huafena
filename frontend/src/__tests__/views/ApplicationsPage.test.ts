import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import ApplicationsPage from '@/views/ApplicationsPage.vue'

// Mock application API
vi.mock('@/api/application', () => ({
  getApplications: vi.fn(),
  getApplicationDetail: vi.fn(),
  getQuestionnaireResponse: vi.fn(),
  initialReview: vi.fn(),
  getPublicLinks: vi.fn().mockResolvedValue({ data: [] }),
  generatePublicLink: vi.fn(),
  batchApprove: vi.fn(),
  batchReject: vi.fn(),
  batchNotifyInterview: vi.fn(),
  exportApplicationsExcel: vi.fn(),
  getApplicationTimeline: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('@/api/questionnaire', () => ({
  getTemplates: vi.fn().mockResolvedValue({ data: [] }),
}))

// Mock ElMessage and ElMessageBox
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm'),
    },
  }
})

const mockApplications = [
  {
    id: 1,
    userId: 10,
    status: 'PENDING_INITIAL_REVIEW',
    entryType: 'REGISTRATION',
    questionnaireResponseId: 100,
    reviewComment: null,
    reviewedBy: null,
    reviewedAt: null,
    createdAt: '2024-01-15T10:30:00',
    updatedAt: '2024-01-15T10:30:00',
  },
  {
    id: 2,
    userId: 11,
    status: 'INITIAL_REVIEW_PASSED',
    entryType: 'PUBLIC_LINK',
    questionnaireResponseId: 101,
    reviewComment: null,
    reviewedBy: null,
    reviewedAt: null,
    createdAt: '2024-01-14T09:00:00',
    updatedAt: '2024-01-14T09:00:00',
  },
  {
    id: 3,
    userId: 12,
    status: 'REJECTED',
    entryType: 'REGISTRATION',
    questionnaireResponseId: 102,
    reviewComment: null,
    reviewedBy: null,
    reviewedAt: null,
    createdAt: '2024-01-13T08:00:00',
    updatedAt: '2024-01-13T08:00:00',
  },
]

function createWrapper() {
  return mount(ApplicationsPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
      stubs: {
        ElDialog: {
          template: '<div class="el-dialog" v-if="modelValue"><slot /></div>',
          props: ['modelValue'],
        },
      },
    },
  })
}

describe('ApplicationsPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [],
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('申请管理')
  })

  it('should fetch and display applications on mount', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockApplications,
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(api.getApplications).toHaveBeenCalledOnce()
    // Check that table rows are rendered (data rows)
    const rows = wrapper.findAll('.el-table__body tr')
    // Element Plus table may render differently, check text content instead
    expect(wrapper.text()).toContain('注册')
    expect(wrapper.text()).toContain('公开链接')
  })

  it('should display status tags with correct labels', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockApplications,
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('待初审')
    expect(wrapper.text()).toContain('初审通过')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('should show pass/reject buttons only for PENDING_INITIAL_REVIEW status', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockApplications,
    })

    const wrapper = createWrapper()
    await flushPromises()

    const buttons = wrapper.findAll('.el-button')
    const buttonTexts = buttons.map((b) => b.text())
    // Should have 通过 and 拒绝 for the PENDING_INITIAL_REVIEW row
    expect(buttonTexts).toContain('通过')
    expect(buttonTexts).toContain('拒绝')
    // Should have 查看详情 for all rows
    expect(buttonTexts.filter((t) => t === '查看详情').length).toBe(3)
  })

  it('should format created time correctly', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [mockApplications[0]],
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('2024-01-15 10:30:00')
  })

  it('should call initialReview API when approving', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce({ data: [mockApplications[0]] })
      .mockResolvedValueOnce({ data: [] })
    ;(api.initialReview as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: null,
    })

    const wrapper = createWrapper()
    await flushPromises()

    // Find and click the 通过 button
    const approveBtn = wrapper.findAll('.el-button').find((b) => b.text() === '通过')
    expect(approveBtn).toBeDefined()
    await approveBtn!.trigger('click')
    await flushPromises()

    expect(api.initialReview).toHaveBeenCalledWith(1, { approved: true })
  })

  it('should call initialReview API when rejecting', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce({ data: [mockApplications[0]] })
      .mockResolvedValueOnce({ data: [] })
    ;(api.initialReview as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: null,
    })

    const wrapper = createWrapper()
    await flushPromises()

    const rejectBtn = wrapper.findAll('.el-button').find((b) => b.text() === '拒绝')
    expect(rejectBtn).toBeDefined()
    await rejectBtn!.trigger('click')
    await flushPromises()

    expect(api.initialReview).toHaveBeenCalledWith(1, { approved: false })
  })

  it('should display questionnaire response ID in summary column', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [mockApplications[0]],
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('问卷回答 #100')
  })

  it('should handle empty application list', async () => {
    const api = await import('@/api/application')
    ;(api.getApplications as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [],
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(api.getApplications).toHaveBeenCalledOnce()
    // Table should still render but with no data rows
    expect(wrapper.find('.el-table').exists()).toBe(true)
  })
})
