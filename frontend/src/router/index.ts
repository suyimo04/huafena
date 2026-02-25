import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'

declare module 'vue-router' {
  interface RouteMeta {
    /** If true, the route requires authentication */
    requiresAuth?: boolean
    /** Roles allowed to access this route. Empty / undefined = any authenticated user */
    roles?: string[]
  }
}

const routes: RouteRecordRaw[] = [
  // ---- Public routes (no auth) ----
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterPage.vue'),
  },
  {
    path: '/public/questionnaire/:linkToken',
    name: 'PublicQuestionnaire',
    component: () => import('@/views/PublicQuestionnairePage.vue'),
  },

  // ---- Protected routes (wrapped in MainLayout) ----
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/MembersPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      },
      {
        path: 'applications',
        name: 'Applications',
        component: () => import('@/views/ApplicationsPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      },
      {
        path: 'questionnaires',
        name: 'Questionnaires',
        component: () => import('@/views/QuestionnairesPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      },
      {
        path: 'interviews',
        name: 'Interviews',
        component: () => import('@/views/InterviewsPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      },
      {
        path: 'points',
        name: 'Points',
        component: () => import('@/views/PointsPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'MEMBER', 'INTERN'] },
      },
      {
        path: 'salary',
        name: 'Salary',
        component: () => import('@/views/SalaryPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'VICE_LEADER', 'MEMBER'] },
      },
      {
        path: 'rotation',
        name: 'Rotation',
        component: () => import('@/views/RotationPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER'] },
      },
      {
        path: 'activities',
        name: 'Activities',
        component: () => import('@/views/ActivitiesPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'MEMBER', 'INTERN'] },
      },
      {
        path: 'emails',
        name: 'Emails',
        component: () => import('@/views/EmailManagementPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER'] },
      },
      {
        path: 'internships',
        name: 'Internships',
        component: () => import('@/views/InternshipManagementPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/SystemUsersPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * Determine the first accessible route for a given role.
 * Falls back to '/login' if nothing matches (shouldn't happen for valid roles).
 */
export function getFirstAccessibleRoute(role: string): string {
  const childRoutes = routes
    .find((r) => r.path === '/')
    ?.children?.filter((c): c is RouteRecordRaw & { path: string } => typeof c.path === 'string' && c.path !== '')

  if (!childRoutes) return '/login'

  for (const child of childRoutes) {
    const roles = child.meta?.roles as string[] | undefined
    if (!roles || roles.includes(role)) {
      return `/${child.path}`
    }
  }
  return '/login'
}

/**
 * Check whether a role is allowed to access a route based on its meta.roles.
 */
export function isRoleAllowed(role: string, allowedRoles?: string[]): boolean {
  if (!allowedRoles || allowedRoles.length === 0) return true
  return allowedRoles.includes(role)
}

export { routes }
export default router

/**
 * Install the navigation guard. Must be called after Pinia is installed.
 */
export function setupRouterGuard() {
  router.beforeEach(async (to, _from) => {
    // Lazy-import to avoid accessing Pinia before it's installed
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()

    // Determine if the target route (or any matched parent) requires auth
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

    // Public routes – allow freely
    if (!requiresAuth) {
      // If already logged in and going to login/register, redirect to dashboard
      if (authStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
        return getFirstAccessibleRoute(authStore.role)
      }
      return true
    }

    // Protected route but not logged in → redirect to login
    if (!authStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    // Check role-based access on the deepest matched route with roles defined
    const allowedRoles = to.meta.roles as string[] | undefined
    if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(authStore.role)) {
      // Redirect to first accessible route for this role
      return getFirstAccessibleRoute(authStore.role)
    }

    return true
  })
}
