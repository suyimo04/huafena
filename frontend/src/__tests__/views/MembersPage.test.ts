import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import MembersPage from '@/views/MembersPage.vue'

vi.mock('@/api/members', () => ({
  checkPromotionEligibility: vi.fn(),
  checkDemotionCandidates: vi.fn(),
  triggerPromotionReview: vi.fn(),
  executePromotion: vi.fn(),
  markForDismissal: vi.fn(),
  getPendingDismissalList: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm'),
    },
  }
})

const mockInterns = [
  { id: 1, username: 'intern1', role: 'INTERN', enabled: true, pendingDismissal: false, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
]

const mockDemotionCandidates = [
  { id: 2, username: 'member1', role: 'MEMBER', enabled: true, pendingDismissal: false, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
]

const mockPendingDismissal = [
  { id: 3, username: 'intern2', role: 'INTERN', enabled: true, pendingDismissal: true, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
]

function createWrapper() {
  return mount(MembersPage, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

function mockAllApis(api: any) {
  ;(api.checkPromotionEligibility as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockInterns })
  ;(api.checkDemotionCandidates as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockDemotionCandidates })
  ;(api.getPendingDismissalList as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPendingDismissal })
}

describe('MembersPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should render page title', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('成员管理与流转')
  })

  it('should render three tabs', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('成员列表')
    expect(wrapper.text()).toContain('转正评议')
    expect(wrapper.text()).toContain('待开除')
  })

  it('should load member data on mount', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    createWrapper()
    await flushPromises()
    expect(api.checkPromotionEligibility).toHaveBeenCalled()
    expect(api.checkDemotionCandidates).toHaveBeenCalled()
    expect(api.getPendingDismissalList).toHaveBeenCalled()
  })

  it('should display members in the list', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('intern1')
    expect(wrapper.text()).toContain('member1')
    expect(wrapper.text()).toContain('intern2')
  })

  it('should show eligible intern label', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('符合转正条件')
  })

  it('should show demotion candidate label', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('薪酬不达标')
  })

  it('should handle empty data gracefully', async () => {
    const api = await import('@/api/members')
    ;(api.checkPromotionEligibility as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })
    ;(api.checkDemotionCandidates as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })
    ;(api.getPendingDismissalList as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.allMembers).toEqual([])
  })

  it('should handle API errors gracefully', async () => {
    const api = await import('@/api/members')
    ;(api.checkPromotionEligibility as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('fail'))
    ;(api.checkDemotionCandidates as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('fail'))
    ;(api.getPendingDismissalList as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('fail'))
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.allMembers).toEqual([])
  })

  it('should trigger promotion review', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    ;(api.triggerPromotionReview as ReturnType<typeof vi.fn>).mockResolvedValue({ data: true })
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleTriggerReview()
    await flushPromises()
    expect(api.triggerPromotionReview).toHaveBeenCalled()
  })

  it('should show warning when promotion conditions not met', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    ;(api.triggerPromotionReview as ReturnType<typeof vi.fn>).mockResolvedValue({ data: false })
    const { ElMessage } = await import('element-plus')
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleTriggerReview()
    await flushPromises()
    expect(ElMessage.warning).toHaveBeenCalledWith('当前不满足转正评议条件')
  })

  it('should execute promotion', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    ;(api.executePromotion as ReturnType<typeof vi.fn>).mockResolvedValue({ data: null })
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.promotionForm.internId = 1
    vm.promotionForm.memberId = 2
    await vm.handleExecutePromotion()
    await flushPromises()
    expect(api.executePromotion).toHaveBeenCalledWith({ internId: 1, memberId: 2 })
  })

  it('should mark members for dismissal', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    ;(api.markForDismissal as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPendingDismissal })
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleMarkDismissal()
    await flushPromises()
    expect(api.markForDismissal).toHaveBeenCalled()
  })

  it('should show info when no members to mark for dismissal', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    ;(api.markForDismissal as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })
    const { ElMessage } = await import('element-plus')
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleMarkDismissal()
    await flushPromises()
    expect(ElMessage.info).toHaveBeenCalledWith('当前没有需要标记的成员')
  })

  it('roleTagType returns correct types', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.roleTagType('ADMIN')).toBe('danger')
    expect(vm.roleTagType('LEADER')).toBe('warning')
    expect(vm.roleTagType('MEMBER')).toBe('success')
    expect(vm.roleTagType('INTERN')).toBe('info')
    expect(vm.roleTagType('UNKNOWN')).toBe('info')
  })

  it('should display pending dismissal members', async () => {
    const api = await import('@/api/members')
    mockAllApis(api)
    const wrapper = createWrapper()
    await flushPromises()
    // The pending dismissal member should appear in the member list with 待开除 tag
    expect(wrapper.text()).toContain('intern2')
    expect(wrapper.text()).toContain('待开除')
  })

  it('should deduplicate members in allMembers', async () => {
    const api = await import('@/api/members')
    const sharedMember = { id: 1, username: 'shared', role: 'INTERN', enabled: true, pendingDismissal: false, createdAt: '2024-01-01', updatedAt: '2024-01-01' }
    ;(api.checkPromotionEligibility as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [sharedMember] })
    ;(api.checkDemotionCandidates as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] })
    ;(api.getPendingDismissalList as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [sharedMember] })
    const wrapper = createWrapper()
    await flushPromises()
    const vm = wrapper.vm as any
    // Same id should only appear once
    expect(vm.allMembers.length).toBe(1)
  })
})
