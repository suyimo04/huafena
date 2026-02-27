import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import SalaryPage from '../SalaryPage.vue'

vi.mock('@/api/salary', () => ({
  getSalaryPeriods: vi.fn(),
  createSalaryPeriod: vi.fn(),
  getSalaryMembers: vi.fn(),
  batchSaveSalary: vi.fn(),
  calculateAndDistribute: vi.fn(),
  archiveSalary: vi.fn(),
}))

vi.mock('@/api/salaryConfig', () => ({
  getCheckinTiers: vi.fn().mockResolvedValue({ data: [] }),
  getSalaryConfig: vi.fn().mockResolvedValue({ data: {} }),
  updateSalaryConfig: vi.fn().mockResolvedValue({}),
  updateCheckinTiers: vi.fn().mockResolvedValue({}),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn().mockResolvedValue('confirm') },
  }
})

function makeMember(overrides: Partial<any> = {}) {
  return {
    id: 1, userId: 10, username: '张三', role: 'LEADER',
    basePoints: 0, bonusPoints: 0, deductions: 0, totalPoints: 0,
    miniCoins: 0, salaryAmount: 0, remark: null, version: 1,
    communityActivityPoints: 50, checkinCount: 35, checkinPoints: 0,
    violationHandlingCount: 2, violationHandlingPoints: 6,
    taskCompletionPoints: 8, announcementCount: 3, announcementPoints: 15,
    eventHostingPoints: 15, birthdayBonusPoints: 25, monthlyExcellentPoints: 20,
    ...overrides,
  }
}

const mockMembers = [
  makeMember({ id: 1, userId: 10, username: '张三', role: 'LEADER' }),
  makeMember({ id: 2, userId: 11, username: '李四', role: 'VICE_LEADER' }),
]

function createWrapper() {
  return mount(SalaryPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('SalaryPage - period selector logic', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('defaults to the latest non-archived period on mount', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: true, recordCount: 3 },
        { period: '2025-06', archived: false, recordCount: 3 },
        { period: '2025-05', archived: true, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.currentPeriod).toBe('2025-06')
  })

  it('falls back to latest period when all periods are archived', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: true, recordCount: 3 },
        { period: '2025-06', archived: true, recordCount: 2 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.currentPeriod).toBe('2025-07')
  })

  it('reloads members when period is switched via onPeriodChange', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: false, recordCount: 3 },
        { period: '2025-06', archived: false, recordCount: 2 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const initialCallCount = (api.getSalaryMembers as ReturnType<typeof vi.fn>).mock.calls.length

    const vm = wrapper.vm as any
    vm.currentPeriod = '2025-06'
    await vm.onPeriodChange()
    await flushPromises()

    expect((api.getSalaryMembers as ReturnType<typeof vi.fn>).mock.calls.length).toBeGreaterThan(initialCallCount)
  })
})

describe('SalaryPage - archived period UI disable', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('isCurrentPeriodArchived is true when selected period is archived', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: true, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.isCurrentPeriodArchived).toBe(true)
  })

  it('isCurrentPeriodArchived is false when selected period is active', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: false, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.isCurrentPeriodArchived).toBe(false)
  })

  it('startEdit is blocked when period is archived', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: true, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.startEdit(mockMembers[0], 'communityActivityPoints')
    // editingCell should remain null because period is archived
    expect(vm.editingCell).toBeNull()
  })
})

describe('SalaryPage - API calls pass currentPeriod', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchMembers passes the selected period to getSalaryMembers', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: false, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const calls = (api.getSalaryMembers as ReturnType<typeof vi.fn>).mock.calls
    const lastCall = calls[calls.length - 1]
    expect(lastCall[0]).toBe('2025-07')
  })

  it('handleCalculate passes currentPeriod to calculateAndDistribute', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: false, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockMembers })
    ;(api.calculateAndDistribute as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.handleCalculate()
    await flushPromises()

    expect(api.calculateAndDistribute).toHaveBeenCalledWith('2025-07')
  })

  it('handleArchive passes currentPeriod to archiveSalary', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: false, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockMembers })
    ;(api.archiveSalary as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: 2 })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.handleArchive()
    await flushPromises()

    const calls = (api.archiveSalary as ReturnType<typeof vi.fn>).mock.calls
    expect(calls.length).toBeGreaterThan(0)
    // archiveSalary(operatorId, period)
    expect(calls[0][1]).toBe('2025-07')
  })

  it('handleBatchSave passes currentPeriod to batchSaveSalary', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryPeriods as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        { period: '2025-07', archived: false, recordCount: 3 },
      ],
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockMembers })
    ;(api.batchSaveSalary as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { success: true, savedRecords: [], errors: [], globalError: null, violatingUserIds: [] },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(10)
    editRow.remark = 'test'
    await vm.handleBatchSave()
    await flushPromises()

    const calls = (api.batchSaveSalary as ReturnType<typeof vi.fn>).mock.calls
    expect(calls.length).toBeGreaterThan(0)
    // batchSaveSalary(data, period)
    expect(calls[0][1]).toBe('2025-07')
  })
})
