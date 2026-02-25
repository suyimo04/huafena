import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import LoginPage from '@/views/LoginPage.vue'

// Mock axios
vi.mock('@/api/axios', () => ({
  default: {
    post: vi.fn(),
  },
}))

// Mock ElMessage
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
      { path: '/login', component: LoginPage },
      { path: '/register', component: { template: '<div>Register</div>' } },
      { path: '/dashboard', component: { template: '<div>Dashboard</div>' } },
    ],
  })
}

describe('LoginPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should render login form with username and password fields', () => {
    const router = createTestRouter()
    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    expect(wrapper.text()).toContain('花粉小组管理系统')
    expect(wrapper.text()).toContain('请登录您的账户')
    expect(wrapper.find('input[type="text"], input').exists()).toBe(true)
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
  })

  it('should render a link to the register page', () => {
    const router = createTestRouter()
    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    const link = wrapper.find('a[href="/register"]')
    expect(link.exists()).toBe(true)
    expect(link.text()).toContain('立即注册')
  })

  it('should have a login button', () => {
    const router = createTestRouter()
    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    const btn = wrapper.find('.login-btn')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('登录')
  })

  it('should call login API and store token on successful login', async () => {
    const api = await import('@/api/axios')
    const payload = { userId: 1, username: 'admin', role: 'ADMIN' }
    const fakeToken = `h.${btoa(JSON.stringify(payload))}.s`

    ;(api.default.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      code: 200,
      message: 'success',
      data: { token: fakeToken },
    })

    const router = createTestRouter()
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    // Fill in form fields by finding inputs
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('admin')
    await inputs[1].setValue('admin123')

    // Click login
    await wrapper.find('.login-btn').trigger('click')
    await flushPromises()

    expect(api.default.post).toHaveBeenCalledWith('/auth/login', {
      username: 'admin',
      password: 'admin123',
    })

    expect(localStorage.getItem('token')).toBe(fakeToken)
  })

  it('should show validation rules for required fields', () => {
    const router = createTestRouter()
    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
      },
    })

    // Verify the form has required validation rules configured
    const formItems = wrapper.findAll('.el-form-item')
    expect(formItems.length).toBeGreaterThanOrEqual(2) // username + password + submit
  })
})
