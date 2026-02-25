import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import SalaryPage from '@/views/SalaryPage.vue'

vi.mock('@/api/salary', () => ({
  getSalaryList: vi.fn(),
  batchSaveSalary: vi.fn(),
  calculateSalaries: vi.fn(),
  archiveSalary: vi.fn(),
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
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm'),
    },
  }
})

const mockRecords = [
  { id: 1, userId: 10, basePoints: 100, bonusPoints: 20, deductions: 10, totalPoints: 110, miniCoins: 300, salaryAmount: 150, remark: null, version: 1, archived: false },
  { id: 2, userId: 11, basePoints: 80, bonusPoints: 30, deductions: 5, totalPoints: 105, miniCoins: 350, salaryAmount: 175, remark: '优秀', version: 1, archived: false },
  { id: 3, userId: 12, basePoints: 90, bonusPoints: 10, deductions: 0, totalPoints: 100, miniCoins: 250, salaryAmount: 125, remark: null, version: 1, archived: false },
  { id: 4, userId: 13, basePoints: 70, bonusPoints: 15, deductions: 5, totalPoints: 80, miniCoins: 400, salaryAmount: 200, remark: null, version: 1, archived: false },
  { id: 5, userId: 14, basePoints: 60, bonusPoints: 25, deductions: 10, totalPoints: 75, miniCoins: 200, salaryAmount: 100, remark: null, version: 1, archived: false },
]

function createWrapper() {
  return mount(SalaryPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('SalaryPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('薪资管理')
  })

  it('should load and display salary records', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    expect(api.getSalaryList).toHaveBeenCalled()
    // Check some data is rendered
    expect(wrapper.text()).toContain('300')
    expect(wrapper.text()).toContain('350')
    expect(wrapper.text()).toContain('优秀')
  })

  it('should show edited count when rows are modified', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // Simulate editing a row
    vm.getEditRow(1)
    await flushPromises()

    expect(vm.editedCount).toBe(1)
    expect(wrapper.text()).toContain('1 条未保存修改')
  })

  it('should track multiple edited rows', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.getEditRow(1)
    vm.getEditRow(2)
    vm.getEditRow(3)
    await flushPromises()

    expect(vm.editedCount).toBe(3)
    expect(wrapper.text()).toContain('3 条未保存修改')
  })

  it('should validate mini-coins range on batch save', async () => {
    const api = await import('@/api/salary')
    const badRecords = mockRecords.map((r) =>
      r.id === 1 ? { ...r, miniCoins: 100 } : r,
    )
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: badRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // Edit the bad row to trigger it being in editedRows
    vm.getEditRow(1)
    await vm.handleBatchSave()
    await flushPromises()

    expect(vm.globalError).toContain('不在 [200, 400] 范围内')
    expect(api.batchSaveSalary).not.toHaveBeenCalled()
  })

  it('should validate total mini-coins on batch save', async () => {
    const api = await import('@/api/salary')
    // All at 400 = 2000, but make one 450 to exceed
    const overRecords = mockRecords.map((r) => ({ ...r, miniCoins: 410 }))
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: overRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.getEditRow(1)
    await vm.handleBatchSave()
    await flushPromises()

    expect(vm.globalError).toContain('超过 2000 上限')
  })

  it('should validate member count on batch save', async () => {
    const api = await import('@/api/salary')
    const shortRecords = mockRecords.slice(0, 3)
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: shortRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.getEditRow(1)
    await vm.handleBatchSave()
    await flushPromises()

    expect(vm.globalError).toContain('应为 5 人')
  })

  it('should call batchSaveSalary when validation passes', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })
    ;(api.batchSaveSalary as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { success: true, savedRecords: mockRecords, errors: [], globalError: null, violatingUserIds: [] },
    })
    // After save, re-fetch
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // Edit a row
    const editRow = vm.getEditRow(1)
    editRow.remark = 'test'
    await vm.handleBatchSave()
    await flushPromises()

    expect(api.batchSaveSalary).toHaveBeenCalled()
    expect(vm.editedCount).toBe(0)
  })

  it('should handle empty records gracefully', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.records).toEqual([])
  })

  it('should handle API errors gracefully', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.records).toEqual([])
  })

  it('should call calculateSalaries on calculate button', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockRecords })
    ;(api.calculateSalaries as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.handleCalculate()
    await flushPromises()

    expect(api.calculateSalaries).toHaveBeenCalled()
  })

  it('should display server-side validation errors from batch save', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })
    ;(api.batchSaveSalary as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: {
        success: false,
        savedRecords: null,
        errors: [{ userId: 10, field: 'miniCoins', message: '超出范围' }],
        globalError: '验证失败',
        violatingUserIds: [10],
      },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.getEditRow(1)
    await vm.handleBatchSave()
    await flushPromises()

    expect(vm.globalError).toBe('验证失败')
    expect(vm.violatingIds.size).toBeGreaterThan(0)
  })

  it('getDisplayValue returns edited value when row is edited', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(1)
    editRow.basePoints = 999

    expect(vm.getDisplayValue(mockRecords[0], 'basePoints')).toBe(999)
  })

  it('getDisplayValue returns original value when row is not edited', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryList as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockRecords })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.getDisplayValue(mockRecords[0], 'basePoints')).toBe(100)
  })
})
