import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { routes, getFirstAccessibleRoute, isRoleAllowed } from '@/router/index'

// Helper: create a fresh router with the real routes + guard installed
function createTestRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes,
  })

  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)

    if (!requiresAuth) {
      if (authStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
        return getFirstAccessibleRoute(authStore.role)
      }
      return true
    }

    if (!authStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    const allowedRoles = to.meta.roles as string[] | undefined
    if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(authStore.role)) {
      return getFirstAccessibleRoute(authStore.role)
    }

    return true
  })

  return router
}

// Helper: create a fake JWT for a given role
function fakeJwt(role: string, username = 'testuser') {
  const payload = { userId: 1, username, role }
  return `header.${btoa(JSON.stringify(payload))}.signature`
}

describe('Router - route configuration', () => {
  it('should have /login as a public route', () => {
    const login = routes.find((r) => r.path === '/login')
    expect(login).toBeDefined()
    expect(login?.meta?.requiresAuth).toBeFalsy()
  })

  it('should have /register as a public route', () => {
    const register = routes.find((r) => r.path === '/register')
    expect(register).toBeDefined()
    expect(register?.meta?.requiresAuth).toBeFalsy()
  })

  it('should have /public/questionnaire/:linkToken as a public route', () => {
    const pub = routes.find((r) => r.path === '/public/questionnaire/:linkToken')
    expect(pub).toBeDefined()
    expect(pub?.meta?.requiresAuth).toBeFalsy()
  })

  it('should have protected routes under / with requiresAuth', () => {
    const layout = routes.find((r) => r.path === '/')
    expect(layout).toBeDefined()
    expect(layout?.meta?.requiresAuth).toBe(true)
  })

  it('should define correct roles for dashboard', () => {
    const layout = routes.find((r) => r.path === '/')
    const dashboard = layout?.children?.find(
      (c) => (c as RouteRecordRaw).path === 'dashboard'
    ) as RouteRecordRaw | undefined
    expect(dashboard).toBeDefined()
    expect(dashboard?.meta?.roles).toEqual(['ADMIN', 'LEADER'])
  })

  it('should define correct roles for points', () => {
    const layout = routes.find((r) => r.path === '/')
    const points = layout?.children?.find(
      (c) => (c as RouteRecordRaw).path === 'points'
    ) as RouteRecordRaw | undefined
    expect(points).toBeDefined()
    expect(points?.meta?.roles).toEqual(['ADMIN', 'LEADER', 'MEMBER', 'INTERN'])
  })

  it('should define correct roles for salary', () => {
    const layout = routes.find((r) => r.path === '/')
    const salary = layout?.children?.find(
      (c) => (c as RouteRecordRaw).path === 'salary'
    ) as RouteRecordRaw | undefined
    expect(salary).toBeDefined()
    expect(salary?.meta?.roles).toEqual(['ADMIN', 'LEADER', 'VICE_LEADER', 'MEMBER'])
  })
})

describe('isRoleAllowed', () => {
  it('should allow any role when allowedRoles is undefined', () => {
    expect(isRoleAllowed('INTERN')).toBe(true)
  })

  it('should allow any role when allowedRoles is empty', () => {
    expect(isRoleAllowed('INTERN', [])).toBe(true)
  })

  it('should allow role in the list', () => {
    expect(isRoleAllowed('ADMIN', ['ADMIN', 'LEADER'])).toBe(true)
  })

  it('should deny role not in the list', () => {
    expect(isRoleAllowed('INTERN', ['ADMIN', 'LEADER'])).toBe(false)
  })
})

describe('getFirstAccessibleRoute', () => {
  it('should return /dashboard for ADMIN', () => {
    expect(getFirstAccessibleRoute('ADMIN')).toBe('/dashboard')
  })

  it('should return /dashboard for LEADER', () => {
    expect(getFirstAccessibleRoute('LEADER')).toBe('/dashboard')
  })

  it('should return /members for VICE_LEADER (first route they can access)', () => {
    expect(getFirstAccessibleRoute('VICE_LEADER')).toBe('/members')
  })

  it('should return /points for MEMBER', () => {
    expect(getFirstAccessibleRoute('MEMBER')).toBe('/points')
  })

  it('should return /points for INTERN', () => {
    expect(getFirstAccessibleRoute('INTERN')).toBe('/points')
  })
})

describe('Navigation guard', () => {
  let router: ReturnType<typeof createTestRouter>

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    router = createTestRouter()
  })

  it('should redirect unauthenticated user to /login for protected routes', async () => {
    await router.push('/dashboard')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/dashboard')
  })

  it('should allow unauthenticated user to access /login', async () => {
    await router.push('/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('should allow unauthenticated user to access /register', async () => {
    await router.push('/register')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/register')
  })

  it('should allow unauthenticated user to access public questionnaire', async () => {
    await router.push('/public/questionnaire/abc123')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/public/questionnaire/abc123')
  })

  it('should allow ADMIN to access /dashboard', async () => {
    const authStore = useAuthStore()
    authStore.setToken(fakeJwt('ADMIN'))
    authStore.loadUserFromToken()

    await router.push('/dashboard')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('should redirect INTERN away from /dashboard to their first accessible route', async () => {
    const authStore = useAuthStore()
    authStore.setToken(fakeJwt('INTERN'))
    authStore.loadUserFromToken()

    await router.push('/dashboard')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/points')
  })

  it('should redirect MEMBER away from /rotation to their first accessible route', async () => {
    const authStore = useAuthStore()
    authStore.setToken(fakeJwt('MEMBER'))
    authStore.loadUserFromToken()

    await router.push('/rotation')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/points')
  })

  it('should allow VICE_LEADER to access /salary', async () => {
    const authStore = useAuthStore()
    authStore.setToken(fakeJwt('VICE_LEADER'))
    authStore.loadUserFromToken()

    await router.push('/salary')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/salary')
  })

  it('should redirect logged-in user from /login to first accessible route', async () => {
    const authStore = useAuthStore()
    authStore.setToken(fakeJwt('LEADER'))
    authStore.loadUserFromToken()

    await router.push('/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('should redirect logged-in user from /register to first accessible route', async () => {
    const authStore = useAuthStore()
    authStore.setToken(fakeJwt('MEMBER'))
    authStore.loadUserFromToken()

    await router.push('/register')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/points')
  })
})
