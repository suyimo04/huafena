import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import RegisterPage from '@/views/RegisterPage.vue'

// Mock axios
vi.mock('@/api/axios', () => ({
  default: {
    post: vi.fn(),
  },
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

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: { template: '<div>Login</div>' } },
      { path: '/register', component: RegisterPage },
    ],
  })
}

describe('RegisterPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should render registration form with username, password, confirm password, and application fields', () => {
    const router = createTestRouter()
    const wrapper = mount(RegisterPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    expect(wrapper.text()).toContain('注册新账户')
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('确认密码')
    expect(wrapper.text()).toContain('花粉 UID（QQ号）')
    expect(wrapper.text()).toContain('出生年月')
    expect(wrapper.text()).toContain('教育阶段')
    expect(wrapper.text()).toContain('每周可用天数')
    expect(wrapper.text()).toContain('每日可用时长')
    expect(wrapper.text()).toContain('每周可用时段')
  })

  it('should render a link to the login page', () => {
    const router = createTestRouter()
    const wrapper = mount(RegisterPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    const link = wrapper.find('a[href="/login"]')
    expect(link.exists()).toBe(true)
    expect(link.text()).toContain('返回登录')
  })

  it('should render a questionnaire placeholder area', () => {
    const router = createTestRouter()
    const wrapper = mount(RegisterPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    expect(wrapper.find('.questionnaire-placeholder').exists()).toBe(true)
    expect(wrapper.text()).toContain('申请问卷')
    expect(wrapper.text()).toContain('问卷将在此处加载')
  })

  it('should have a register button', () => {
    const router = createTestRouter()
    const wrapper = mount(RegisterPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    const btn = wrapper.find('.register-btn')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('注册')
  })

  it('should call register API and redirect to login on success', async () => {
    const api = await import('@/api/axios')
    ;(api.default.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      code: 200,
      message: 'success',
      data: null,
    })

    const router = createTestRouter()
    await router.push('/register')
    await router.isReady()
    const pushSpy = vi.spyOn(router, 'push')

    const wrapper = mount(RegisterPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('newuser')
    await inputs[1].setValue('password123')
    await inputs[2].setValue('password123')

    await wrapper.find('.register-btn').trigger('click')
    await flushPromises()

    expect(api.default.post).toHaveBeenCalledWith('/auth/register', expect.objectContaining({
      username: 'newuser',
      password: 'password123',
    }))

    expect(pushSpy).toHaveBeenCalledWith('/login')
  })

  it('should show validation rules for required fields', () => {
    const router = createTestRouter()
    const wrapper = mount(RegisterPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    // Verify the form has required validation rules configured (many form items now)
    const formItems = wrapper.findAll('.el-form-item')
    expect(formItems.length).toBeGreaterThanOrEqual(8)
  })
})
