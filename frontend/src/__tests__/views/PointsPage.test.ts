import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import PointsPage from '@/views/PointsPage.vue'

vi.mock('@/api/points', () => ({
  getPointsRecords: vi.fn(),
  getTotalPoints: vi.fn(),
  addPoints: vi.fn(),
  deductPoints: vi.fn(),
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

const mockRecords = [
  {
    id: 1,
    userId: 1,
    pointsType: 'COMMUNITY_ACTIVITY',
    amount: 50,
    description: '月度活跃',
    createdAt: '2024-06-01T10:00:00',
  },
  {
    id: 2,
    userId: 1,
    pointsType: 'CHECKIN',
    amount: -10,
    description: '签到不足',
    createdAt: '2024-06-02T10:00:00',
  },
  {
    id: 3,
    userId: 1,
    pointsType: 'TASK_COMPLETION',
    amount: 5,
    description: '完成任务',
    createdAt: '2024-06-03T10:00:00',
  },
]

function createWrapper() {
  return mount(PointsPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('PointsPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title and stats cards', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('积分管理')
    expect(wrapper.text()).toContain('总积分')
    expect(wrapper.text()).toContain('记录数')
  })

  it('should render type filter dropdown', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.el-select').exists()).toBe(true)
  })

  it('should display records in table after fetch', async () => {
    const api = await import('@/api/points')
    ;(api.getPointsRecords as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })
    ;(api.getTotalPoints as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: 45 })

    const wrapper = createWrapper()

    // Simulate query
    const input = wrapper.find('.el-input input')
    await input.setValue(1)
    // Trigger fetch via the query button
    const queryBtn = wrapper.findAll('.el-button').find((b) => b.text() === '查询')
    await queryBtn!.trigger('click')
    await flushPromises()

    expect(api.getPointsRecords).toHaveBeenCalledWith(1)
    expect(api.getTotalPoints).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('月度活跃')
    expect(wrapper.text()).toContain('签到不足')
    expect(wrapper.text()).toContain('45')
  })

  it('should show positive amounts in green and negative in red', async () => {
    const api = await import('@/api/points')
    ;(api.getPointsRecords as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })
    ;(api.getTotalPoints as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: 45 })

    const wrapper = createWrapper()
    const input = wrapper.find('.el-input input')
    await input.setValue(1)
    const queryBtn = wrapper.findAll('.el-button').find((b) => b.text() === '查询')
    await queryBtn!.trigger('click')
    await flushPromises()

    const greenSpans = wrapper.findAll('.text-emerald-600')
    const redSpans = wrapper.findAll('.text-red-500')
    // At least one positive and one negative
    expect(greenSpans.length).toBeGreaterThan(0)
    expect(redSpans.length).toBeGreaterThan(0)
  })

  it('should filter records by type', async () => {
    const api = await import('@/api/points')
    ;(api.getPointsRecords as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })
    ;(api.getTotalPoints as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: 45 })

    const wrapper = createWrapper()
    const input = wrapper.find('.el-input input')
    await input.setValue(1)
    const queryBtn = wrapper.findAll('.el-button').find((b) => b.text() === '查询')
    await queryBtn!.trigger('click')
    await flushPromises()

    // All 3 records visible initially
    const rows = wrapper.findAll('.el-table__row')
    expect(rows.length).toBe(3)

    // Set filter type via component data
    const vm = wrapper.vm as any
    vm.filterType = 'CHECKIN'
    await flushPromises()

    // filteredRecords should have 1 record
    expect(vm.filteredRecords.length).toBe(1)
    expect(vm.filteredRecords[0].pointsType).toBe('CHECKIN')
  })

  it('should format time correctly', () => {
    const wrapper = createWrapper()
    const vm = wrapper.vm as any
    expect(vm.formatTime('2024-06-01T10:00:00')).toBe('2024-06-01 10:00:00')
    expect(vm.formatTime('')).toBe('')
  })

  it('should map points type to Chinese label', () => {
    const wrapper = createWrapper()
    const vm = wrapper.vm as any
    expect(vm.typeLabel('COMMUNITY_ACTIVITY')).toBe('社群活跃度')
    expect(vm.typeLabel('CHECKIN')).toBe('签到奖惩')
    expect(vm.typeLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('should not show add/deduct buttons for non-admin/leader roles', () => {
    const wrapper = createWrapper()
    // Default pinia has no user, so canManagePoints is false
    const addBtn = wrapper.findAll('.el-button').find((b) => b.text() === '增加积分')
    expect(addBtn).toBeUndefined()
  })

  it('should handle empty records gracefully', async () => {
    const api = await import('@/api/points')
    ;(api.getPointsRecords as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })
    ;(api.getTotalPoints as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: 0 })

    const wrapper = createWrapper()
    const input = wrapper.find('.el-input input')
    await input.setValue(999)
    const queryBtn = wrapper.findAll('.el-button').find((b) => b.text() === '查询')
    await queryBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('0')
  })

  it('should handle API errors gracefully', async () => {
    const api = await import('@/api/points')
    ;(api.getPointsRecords as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))
    ;(api.getTotalPoints as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))

    const wrapper = createWrapper()
    const input = wrapper.find('.el-input input')
    await input.setValue(1)
    const queryBtn = wrapper.findAll('.el-button').find((b) => b.text() === '查询')
    await queryBtn!.trigger('click')
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.records).toEqual([])
    expect(vm.totalPoints).toBe(0)
  })
})
