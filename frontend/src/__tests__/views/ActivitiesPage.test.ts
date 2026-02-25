import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import ActivitiesPage from '@/views/ActivitiesPage.vue'

vi.mock('@/api/activity', () => ({
  listActivities: vi.fn(),
  createActivity: vi.fn(),
  registerForActivity: vi.fn(),
  checkInActivity: vi.fn(),
  archiveActivity: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

const mockActivities = [
  {
    id: 1,
    name: '线下聚会',
    description: '周末聚会',
    activityTime: '2024-07-01T14:00:00',
    location: '北京',
    registrationCount: 5,
    status: 'UPCOMING',
    createdBy: 1,
    createdAt: '2024-06-20T10:00:00',
  },
  {
    id: 2,
    name: '线上分享',
    description: '技术分享',
    activityTime: '2024-06-15T19:00:00',
    location: '线上',
    registrationCount: 12,
    status: 'ARCHIVED',
    createdBy: 1,
    createdAt: '2024-06-10T10:00:00',
  },
]

function createWrapper() {
  return mount(ActivitiesPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('ActivitiesPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('活动管理')
  })

  it('should display activities in table after fetch', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockActivities })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('线下聚会')
    expect(wrapper.text()).toContain('北京')
    expect(wrapper.text()).toContain('线上分享')
    expect(wrapper.text()).toContain('共 2 个活动')
  })

  it('should display status tags with correct labels', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockActivities })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('即将开始')
    expect(wrapper.text()).toContain('已归档')
  })

  it('should format activity time correctly', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockActivities })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.formatTime('2024-07-01T14:00:00')).toBe('2024-07-01 14:00')
    expect(vm.formatTime('')).toBe('')
  })

  it('should not show create button for non-admin/leader roles', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockActivities })

    const wrapper = createWrapper()
    await flushPromises()

    // Default pinia has no user, so canManage is false
    const createBtn = wrapper.findAll('.el-button').find((b) => b.text() === '创建活动')
    expect(createBtn).toBeUndefined()
  })

  it('should show register and check-in buttons for non-admin users on non-archived activities', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockActivities })

    const wrapper = createWrapper()
    await flushPromises()

    // Non-admin user: should see register/check-in for UPCOMING activity, not for ARCHIVED
    const buttons = wrapper.findAll('.el-button')
    const registerBtns = buttons.filter((b) => b.text() === '报名')
    const checkInBtns = buttons.filter((b) => b.text() === '签到')

    // Only 1 UPCOMING activity should have register/check-in buttons
    expect(registerBtns.length).toBe(1)
    expect(checkInBtns.length).toBe(1)
  })

  it('should not show register/check-in buttons for archived activities', async () => {
    const archivedOnly = [{ ...mockActivities[1] }]
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: archivedOnly })

    const wrapper = createWrapper()
    await flushPromises()

    const registerBtns = wrapper.findAll('.el-button').filter((b) => b.text() === '报名')
    const checkInBtns = wrapper.findAll('.el-button').filter((b) => b.text() === '签到')
    expect(registerBtns.length).toBe(0)
    expect(checkInBtns.length).toBe(0)
  })

  it('should handle empty activities list', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('共 0 个活动')
  })

  it('should handle API errors gracefully', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.activities).toEqual([])
  })

  it('should map status to correct tag type', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.statusTagType('UPCOMING')).toBe('info')
    expect(vm.statusTagType('ONGOING')).toBe('success')
    expect(vm.statusTagType('COMPLETED')).toBe('warning')
    expect(vm.statusTagType('ARCHIVED')).toBe('')
    expect(vm.statusTagType('UNKNOWN')).toBe('info')
  })

  it('should map status to correct Chinese label', async () => {
    const api = await import('@/api/activity')
    ;(api.listActivities as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.statusLabel('UPCOMING')).toBe('即将开始')
    expect(vm.statusLabel('ONGOING')).toBe('进行中')
    expect(vm.statusLabel('COMPLETED')).toBe('已完成')
    expect(vm.statusLabel('ARCHIVED')).toBe('已归档')
    expect(vm.statusLabel('UNKNOWN')).toBe('UNKNOWN')
  })
})
