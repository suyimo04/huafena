import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import SalaryPage from '@/views/SalaryPage.vue'

vi.mock('@/api/salary', () => ({
  getSalaryMembers: vi.fn(),
  batchSaveSalary: vi.fn(),
  calculateSalaries: vi.fn(),
  calculateAndDistribute: vi.fn(),
  archiveSalary: vi.fn(),
  getSalaryPeriods: vi.fn().mockResolvedValue({ data: [] }),
  createSalaryPeriod: vi.fn(),
}))

vi.mock('@/api/salaryConfig', () => ({
  getCheckinTiers: vi.fn().mockResolvedValue({
    data: [
      { minCount: 0, maxCount: 19, points: -20, label: '不合格' },
      { minCount: 20, maxCount: 29, points: -10, label: '需改进' },
      { minCount: 30, maxCount: 39, points: 0, label: '合格' },
      { minCount: 40, maxCount: 49, points: 30, label: '良好' },
      { minCount: 50, maxCount: 999, points: 50, label: '优秀' },
    ],
  }),
  getSalaryConfig: vi.fn().mockResolvedValue({
    data: {
      salary_pool_total: '2000',
      formal_member_count: '5',
      base_allocation: '400',
      mini_coins_min: '200',
      mini_coins_max: '400',
      points_to_coins_ratio: '2',
      promotion_points_threshold: '100',
      demotion_salary_threshold: '150',
      demotion_consecutive_months: '2',
      dismissal_points_threshold: '100',
      dismissal_consecutive_months: '2',
    },
  }),
  updateSalaryConfig: vi.fn().mockResolvedValue({ data: null }),
  updateCheckinTiers: vi.fn().mockResolvedValue({ data: null }),
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
  makeMember({ id: 2, userId: 11, username: '李四', role: 'VICE_LEADER', remark: '优秀' }),
  makeMember({ id: 3, userId: 12, username: '王五', role: 'INTERN' }),
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
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('薪资管理')
  })

  it('should load and display salary members', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    expect(api.getSalaryMembers).toHaveBeenCalled()
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('李四')
    expect(wrapper.text()).toContain('优秀')
  })

  it('should show role tags', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('组长')
    expect(wrapper.text()).toContain('副组长')
    expect(wrapper.text()).toContain('实习组员')
  })

  it('should show stats cards with role counts', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.roleCount('LEADER')).toBe(1)
    expect(vm.roleCount('VICE_LEADER')).toBe(1)
    expect(vm.roleCount('INTERN')).toBe(1)
  })

  it('should track edited rows', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.getEditRow(10)
    await flushPromises()

    expect(vm.editedCount).toBe(1)
  })

  it('should call batchSaveSalary when saving', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })
    ;(api.batchSaveSalary as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { success: true, savedRecords: [], errors: [], globalError: null, violatingUserIds: [] },
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(10)
    editRow.remark = 'test'
    await vm.handleBatchSave()
    await flushPromises()

    expect(api.batchSaveSalary).toHaveBeenCalled()
    expect(vm.editedCount).toBe(0)
  })

  it('should handle server-side validation errors', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })
    ;(api.batchSaveSalary as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: {
        success: false, savedRecords: null, errors: [],
        globalError: '验证失败', violatingUserIds: [10],
      },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.getEditRow(10)
    await vm.handleBatchSave()
    await flushPromises()

    expect(vm.globalError).toBe('验证失败')
    expect(vm.violatingIds.has(10)).toBe(true)
  })

  it('should call calculateAndDistribute on calculate', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockMembers })
    ;(api.calculateAndDistribute as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.handleCalculate()
    await flushPromises()

    expect(api.calculateAndDistribute).toHaveBeenCalled()
  })

  it('getDisplayValue returns edited value when row is edited', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(10)
    editRow.communityActivityPoints = 99

    expect(vm.getDisplayValue(mockMembers[0], 'communityActivityPoints')).toBe(99)
  })

  it('getDisplayValue returns original value when row is not edited', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.getDisplayValue(mockMembers[0], 'communityActivityPoints')).toBe(50)
  })

  it('should handle empty members gracefully', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.members).toEqual([])
  })

  it('should handle API errors gracefully', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('fail'))

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.members).toEqual([])
  })

  // --- New tests for dimension calculation ---

  it('should compute checkin points using tier lookup', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // checkinCount=35 → tier 30-39 → 0 points
    expect(vm.computeCheckinPoints(mockMembers[0])).toBe(0)
  })

  it('should compute base points correctly from dimensions', async () => {
    const api = await import('@/api/salary')
    const member = makeMember({
      communityActivityPoints: 50, checkinCount: 45, // tier 40-49 → 30
      violationHandlingCount: 2, taskCompletionPoints: 8, announcementCount: 3,
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [member] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // base = 50 + 30 + (2*3) + 8 + (3*5) = 50 + 30 + 6 + 8 + 15 = 109
    expect(vm.computeBasePoints(member)).toBe(109)
  })

  it('should compute bonus points correctly from dimensions', async () => {
    const api = await import('@/api/salary')
    const member = makeMember({
      eventHostingPoints: 15, birthdayBonusPoints: 25, monthlyExcellentPoints: 20,
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [member] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // bonus = 15 + 25 + 20 = 60
    expect(vm.computeBonusPoints(member)).toBe(60)
  })

  it('should compute total points as base + bonus', async () => {
    const api = await import('@/api/salary')
    const member = makeMember({
      communityActivityPoints: 50, checkinCount: 35, // tier 30-39 → 0
      violationHandlingCount: 2, taskCompletionPoints: 8, announcementCount: 3,
      eventHostingPoints: 15, birthdayBonusPoints: 25, monthlyExcellentPoints: 20,
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [member] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // base = 50 + 0 + 6 + 8 + 15 = 79, bonus = 60, total = 139
    expect(vm.computeTotalPoints(member)).toBe(139)
  })

  it('should compute mini coins as totalPoints * 2', async () => {
    const api = await import('@/api/salary')
    const member = makeMember({
      communityActivityPoints: 50, checkinCount: 35,
      violationHandlingCount: 2, taskCompletionPoints: 8, announcementCount: 3,
      eventHostingPoints: 15, birthdayBonusPoints: 25, monthlyExcellentPoints: 20,
    })
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [member] })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.computeMiniCoins(member)).toBe(vm.computeTotalPoints(member) * 2)
  })

  it('should identify intern members correctly', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.isIntern(mockMembers[0])).toBe(false) // LEADER
    expect(vm.isIntern(mockMembers[2])).toBe(true)  // INTERN
  })

  it('should detect validation errors for out-of-range values', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(10)
    editRow.communityActivityPoints = 150 // out of range 0-100

    expect(vm.hasValidationError(10, 'communityActivityPoints')).toBe(true)
    expect(vm.getValidationError(10, 'communityActivityPoints')).toContain('0-100')
  })

  it('should not show validation error for in-range values', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(10)
    editRow.communityActivityPoints = 50

    expect(vm.hasValidationError(10, 'communityActivityPoints')).toBe(false)
  })

  it('should use checkin tiers for different ranges', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    // Test all tiers
    expect(vm.lookupCheckinTier(10)).toBe(-20)  // 0-19 → -20
    expect(vm.lookupCheckinTier(25)).toBe(-10)  // 20-29 → -10
    expect(vm.lookupCheckinTier(35)).toBe(0)    // 30-39 → 0
    expect(vm.lookupCheckinTier(45)).toBe(30)   // 40-49 → 30
    expect(vm.lookupCheckinTier(55)).toBe(50)   // 50+ → 50
  })

  it('should load checkin tiers from API on mount', async () => {
    const api = await import('@/api/salary')
    const configApi = await import('@/api/salaryConfig')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: [] })

    createWrapper()
    await flushPromises()

    expect(configApi.getCheckinTiers).toHaveBeenCalled()
  })

  it('should compute using edited row values when editing', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    const editRow = vm.getEditRow(10)
    editRow.checkinCount = 50 // tier 50+ → 50 points

    expect(vm.computeCheckinPoints(mockMembers[0])).toBe(50)
  })

  // --- Config Drawer tests ---

  it('should have config drawer initially closed', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.configDrawerVisible).toBe(false)
  })

  it('should open config drawer and load config data', async () => {
    const api = await import('@/api/salary')
    const configApi = await import('@/api/salaryConfig')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.openConfigDrawer()
    await flushPromises()

    expect(vm.configDrawerVisible).toBe(true)
    expect(configApi.getSalaryConfig).toHaveBeenCalled()
    expect(vm.configForm.salary_pool_total).toBe(2000)
    expect(vm.configForm.formal_member_count).toBe(5)
    expect(vm.configForm.mini_coins_min).toBe(200)
    expect(vm.configForm.mini_coins_max).toBe(400)
    expect(vm.configForm.promotion_points_threshold).toBe(100)
  })

  it('should load checkin tiers into config drawer', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.openConfigDrawer()
    await flushPromises()

    expect(vm.configCheckinTiers.length).toBe(5)
    expect(vm.configCheckinTiers[0].label).toBe('不合格')
    expect(vm.configCheckinTiers[4].label).toBe('优秀')
  })

  it('should save config and close drawer on success', async () => {
    const api = await import('@/api/salary')
    const configApi = await import('@/api/salaryConfig')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.openConfigDrawer()
    await flushPromises()

    vm.configForm.salary_pool_total = 3000
    await vm.handleSaveConfig()
    await flushPromises()

    expect(configApi.updateSalaryConfig).toHaveBeenCalledWith(
      expect.objectContaining({ salary_pool_total: '3000' })
    )
    expect(configApi.updateCheckinTiers).toHaveBeenCalled()
    expect(vm.configDrawerVisible).toBe(false)
  })

  it('should show error when save config fails', async () => {
    const api = await import('@/api/salary')
    const configApi = await import('@/api/salaryConfig')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })
    ;(configApi.updateSalaryConfig as ReturnType<typeof vi.fn>).mockRejectedValueOnce({
      response: { data: { message: '个人最低迷你币不能大于最高迷你币' } },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.openConfigDrawer()
    await flushPromises()

    await vm.handleSaveConfig()
    await flushPromises()

    expect(vm.configError).toBe('个人最低迷你币不能大于最高迷你币')
    expect(vm.configDrawerVisible).toBe(true)
  })

  it('should refresh local checkin tiers after saving config', async () => {
    const api = await import('@/api/salary')
    ;(api.getSalaryMembers as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ data: mockMembers })

    const wrapper = createWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.openConfigDrawer()
    await flushPromises()

    // Modify a tier in the config
    vm.configCheckinTiers[0].points = -30

    await vm.handleSaveConfig()
    await flushPromises()

    // Local checkinTiers should be updated
    expect(vm.checkinTiers[0].points).toBe(-30)
  })
})
