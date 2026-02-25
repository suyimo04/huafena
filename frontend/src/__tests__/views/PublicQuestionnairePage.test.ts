import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import PublicQuestionnairePage from '@/views/PublicQuestionnairePage.vue'

vi.mock('@/api/questionnaire', () => ({
  getPublicQuestionnaire: vi.fn(),
  submitPublicQuestionnaire: vi.fn(),
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

const mockSchema = {
  groups: [],
  fields: [
    {
      key: 'name',
      type: 'TEXT',
      label: '姓名',
      required: true,
      validationRules: null,
      conditionalLogic: null,
      options: null,
    },
  ],
}

function createTestRouter(token = 'test-token') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/public/questionnaire/:linkToken',
        name: 'PublicQuestionnaire',
        component: PublicQuestionnairePage,
      },
      { path: '/login', component: { template: '<div>Login</div>' } },
    ],
  })
  router.push(`/public/questionnaire/${token}`)
  return router
}

describe('PublicQuestionnairePage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should show loading state initially', async () => {
    const api = await import('@/api/questionnaire')
    ;(api.getPublicQuestionnaire as ReturnType<typeof vi.fn>).mockReturnValue(
      new Promise(() => {}), // never resolves
    )

    const router = createTestRouter()
    await router.isReady()

    const wrapper = mount(PublicQuestionnairePage, {
      global: { plugins: [createPinia(), router, ElementPlus] },
    })

    expect(wrapper.text()).toContain('正在加载问卷')
  })

  it('should render questionnaire after successful fetch', async () => {
    const api = await import('@/api/questionnaire')
    ;(api.getPublicQuestionnaire as ReturnType<typeof vi.fn>).mockResolvedValue({
      code: 200,
      message: 'success',
      data: mockSchema,
    })

    const router = createTestRouter()
    await router.isReady()

    const wrapper = mount(PublicQuestionnairePage, {
      global: { plugins: [createPinia(), router, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('问卷填写')
    expect(wrapper.text()).toContain('姓名')
    expect(wrapper.text()).toContain('花粉 UID（QQ号）')
    expect(wrapper.text()).toContain('出生年月')
    expect(wrapper.text()).toContain('教育阶段')
    expect(wrapper.text()).toContain('每周可用天数')
    expect(wrapper.text()).toContain('每日可用时长')
    expect(wrapper.text()).toContain('每周可用时段')
    expect(api.getPublicQuestionnaire).toHaveBeenCalledWith('test-token')
  })

  it('should show error state when link is invalid', async () => {
    const api = await import('@/api/questionnaire')
    ;(api.getPublicQuestionnaire as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('Not found'),
    )

    const router = createTestRouter('invalid-token')
    await router.isReady()

    const wrapper = mount(PublicQuestionnairePage, {
      global: { plugins: [createPinia(), router, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('链接无效或已过期')
    expect(wrapper.find('.el-button').text()).toContain('重试')
  })

  it('should show success with account info after submission', async () => {
    const api = await import('@/api/questionnaire')
    ;(api.getPublicQuestionnaire as ReturnType<typeof vi.fn>).mockResolvedValue({
      code: 200,
      message: 'success',
      data: mockSchema,
    })
    ;(api.submitPublicQuestionnaire as ReturnType<typeof vi.fn>).mockResolvedValue({
      code: 200,
      message: 'success',
      data: { username: 'user_abc', password: 'pass123' },
    })

    const router = createTestRouter()
    await router.isReady()

    const wrapper = mount(PublicQuestionnairePage, {
      global: { plugins: [createPinia(), router, ElementPlus] },
    })

    await flushPromises()

    // Simulate the QuestionnaireRenderer emitting submit
    // Note: appForm validation is bypassed when appFormRef is not set up in test env
    const renderer = wrapper.findComponent({ name: 'QuestionnaireRenderer' })
    renderer.vm.$emit('submit', { name: '张三' })
    await flushPromises()

    expect(api.submitPublicQuestionnaire).toHaveBeenCalledWith('test-token', expect.objectContaining({ name: '张三' }))
    expect(wrapper.text()).toContain('提交成功')
    expect(wrapper.text()).toContain('user_abc')
    expect(wrapper.text()).toContain('pass123')
  })

  it('should have a button to navigate to login after success', async () => {
    const api = await import('@/api/questionnaire')
    ;(api.getPublicQuestionnaire as ReturnType<typeof vi.fn>).mockResolvedValue({
      code: 200,
      message: 'success',
      data: mockSchema,
    })
    ;(api.submitPublicQuestionnaire as ReturnType<typeof vi.fn>).mockResolvedValue({
      code: 200,
      message: 'success',
      data: { username: 'u1', password: 'p1' },
    })

    const router = createTestRouter()
    await router.isReady()
    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(PublicQuestionnairePage, {
      global: { plugins: [createPinia(), router, ElementPlus] },
    })

    await flushPromises()

    const renderer = wrapper.findComponent({ name: 'QuestionnaireRenderer' })
    renderer.vm.$emit('submit', { name: 'test' })
    await flushPromises()

    const loginBtn = wrapper.findAll('.el-button').find((b) => b.text().includes('前往登录'))
    expect(loginBtn).toBeDefined()
    await loginBtn!.trigger('click')
    expect(pushSpy).toHaveBeenCalledWith('/login')
  })

  it('should retry fetching when retry button is clicked', async () => {
    const api = await import('@/api/questionnaire')
    ;(api.getPublicQuestionnaire as ReturnType<typeof vi.fn>)
      .mockRejectedValueOnce(new Error('fail'))
      .mockResolvedValueOnce({ code: 200, message: 'success', data: mockSchema })

    const router = createTestRouter()
    await router.isReady()

    const wrapper = mount(PublicQuestionnairePage, {
      global: { plugins: [createPinia(), router, ElementPlus] },
    })

    await flushPromises()
    expect(wrapper.text()).toContain('链接无效或已过期')

    await wrapper.find('.el-button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('问卷填写')
    expect(api.getPublicQuestionnaire).toHaveBeenCalledTimes(2)
  })
})
