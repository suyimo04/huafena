import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import DashboardPage from '@/views/DashboardPage.vue'

vi.mock('@/api/dashboard', () => ({
  getDashboardStats: vi.fn(),
  getAuditLogs: vi.fn(),
}))

const mockStats = {
  totalMembers: 20,
  adminCount: 1,
  leaderCount: 1,
  viceLeaderCount: 1,
  memberCount: 4,
  internCount: 3,
  applicantCount: 10,
  totalActivities: 5,
  totalPointsRecords: 150,
}

const mockAuditLogs = [
  {
    id: 1,
    operatorId: 1,
    operationType: 'SALARY_SAVE',
    operationTime: '2024-07-01T14:30:00',
    operationDetail: '批量保存薪资记录',
  },
  {
    id: 2,
    operatorId: 2,
    operationType: 'ROLE_CHANGE',
    operationTime: '2024-06-28T10:00:00',
    operationDetail: '用户 intern1 角色变更为 MEMBER',
  },
]

function createWrapper() {
  return mount(DashboardPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('DashboardPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('数据看板')
  })

  it('should display stats cards with correct values', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('成员总数')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('组长')
    expect(wrapper.text()).toContain('副组长')
    expect(wrapper.text()).toContain('正式成员')
    expect(wrapper.text()).toContain('实习成员')
    expect(wrapper.text()).toContain('申请者')
    expect(wrapper.text()).toContain('活动总数')
    expect(wrapper.text()).toContain('积分记录')
    expect(wrapper.text()).toContain('150')
  })

  it('should display audit logs in table', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockAuditLogs })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('审计日志')
    expect(wrapper.text()).toContain('SALARY_SAVE')
    expect(wrapper.text()).toContain('批量保存薪资记录')
    expect(wrapper.text()).toContain('ROLE_CHANGE')
  })

  it('should format operation time correctly', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockAuditLogs })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.formatTime('2024-07-01T14:30:00')).toBe('2024-07-01 14:30:00')
    expect(vm.formatTime('')).toBe('')
  })

  it('should call getAuditLogs with filter type when selected', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockAuditLogs })

    const wrapper = createWrapper()
    await flushPromises()

    // Initial call without filter
    expect(api.getAuditLogs).toHaveBeenCalledWith(undefined)

    // Simulate filter change
    const vm = wrapper.vm as any
    vm.filterType = 'SALARY_SAVE'
    await vm.fetchAuditLogs()
    await flushPromises()

    expect(api.getAuditLogs).toHaveBeenCalledWith('SALARY_SAVE')
  })

  it('should handle stats API error gracefully', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    // Stats should remain at default 0 values
    const vm = wrapper.vm as any
    expect(vm.stats.totalMembers).toBe(0)
  })

  it('should handle audit logs API error gracefully', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.auditLogs).toEqual([])
  })

  it('should render operation type filter select', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const select = wrapper.find('.el-select')
    expect(select.exists()).toBe(true)
  })

  it('should have correct operation types list', async () => {
    const api = await import('@/api/dashboard')
    ;(api.getDashboardStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockStats })
    ;(api.getAuditLogs as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.operationTypes).toContain('SALARY_SAVE')
    expect(vm.operationTypes).toContain('ROLE_CHANGE')
    expect(vm.operationTypes).toContain('APPLICATION_REVIEW')
    expect(vm.operationTypes).toContain('INTERVIEW_REVIEW')
  })
})
