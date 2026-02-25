<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area">
        <span v-if="!isCollapsed" class="logo-text">花粉管理系统</span>
        <span v-else class="logo-icon">🌸</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :router="true"
        class="sidebar-menu"
        background-color="transparent"
        text-color="#374151"
        active-text-color="#10b981"
        :default-openeds="defaultOpeneds"
        unique-opened
      >
        <template v-for="group in visibleMenuGroups" :key="group.id">
          <!-- 有子菜单的分组 -->
          <el-sub-menu v-if="group.children && group.children.length > 0" :index="group.id">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.title }}</span>
            </template>
            <template v-for="child in group.children" :key="child.path">
              <!-- 三级菜单 -->
              <el-sub-menu v-if="child.children && child.children.length > 0" :index="child.id!">
                <template #title>
                  <el-icon><component :is="child.icon" /></el-icon>
                  <span>{{ child.title }}</span>
                </template>
                <el-menu-item v-for="sub in child.children" :key="sub.path" :index="sub.path!">
                  <el-icon><component :is="sub.icon" /></el-icon>
                  <template #title>{{ sub.title }}</template>
                </el-menu-item>
              </el-sub-menu>
              <!-- 二级叶子菜单 -->
              <el-menu-item v-else :index="child.path!">
                <el-icon><component :is="child.icon" /></el-icon>
                <template #title>{{ child.title }}</template>
              </el-menu-item>
            </template>
          </el-sub-menu>
          <!-- 无子菜单的顶级项 -->
          <el-menu-item v-else :index="group.path!">
            <el-icon><component :is="group.icon" /></el-icon>
            <template #title>{{ group.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- Main area -->
    <el-container class="main-container">
      <!-- Top bar -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-button :icon="isCollapsed ? Expand : Fold" text @click="isCollapsed = !isCollapsed" />
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar :size="32" class="user-avatar">
                {{ userInitial }}
              </el-avatar>
              <span class="username">{{ authStore.user?.username ?? '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  角色：{{ roleLabel }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Content -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, User, Document, ChatDotRound, Coin, Money, Refresh, Calendar, DataAnalysis, Setting, Promotion, List, EditPen, TrendCharts, Notebook, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const userInitial = computed(() => {
  const name = authStore.user?.username ?? '?'
  return name.charAt(0).toUpperCase()
})

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  LEADER: '组长',
  VICE_LEADER: '副组长',
  MEMBER: '正式成员',
  INTERN: '实习成员',
  APPLICANT: '申请者',
}

const roleLabel = computed(() => ROLE_LABELS[authStore.role] ?? authStore.role)

interface MenuNode {
  id: string
  title: string
  icon: any
  path?: string
  roles: string[]
  children?: MenuNode[]
}

const allMenuGroups: MenuNode[] = [
  {
    id: 'dashboard',
    path: '/dashboard',
    title: '数据看板',
    icon: DataAnalysis,
    roles: ['ADMIN', 'LEADER', 'VICE_LEADER', 'MEMBER', 'INTERN'],
  },
  {
    id: 'recruitment',
    title: '招募管理',
    icon: Promotion,
    roles: ['ADMIN', 'LEADER', 'VICE_LEADER'],
    children: [
      { id: 'recruitment-application', path: '/applications', title: '申请管理', icon: Document, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      { id: 'recruitment-questionnaire', path: '/questionnaires', title: '问卷管理', icon: EditPen, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      { id: 'recruitment-interview', path: '/interviews', title: 'AI 面试', icon: ChatDotRound, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
    ],
  },
  {
    id: 'member-mgmt',
    title: '组织管理',
    icon: User,
    roles: ['ADMIN', 'LEADER', 'VICE_LEADER'],
    children: [
      { id: 'member-list', path: '/members', title: '成员列表', icon: List, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
      { id: 'member-rotation', path: '/rotation', title: '成员流转', icon: Refresh, roles: ['ADMIN', 'LEADER'] },
      { id: 'member-internship', path: '/internships', title: '实习管理', icon: Notebook, roles: ['ADMIN', 'LEADER', 'VICE_LEADER'] },
    ],
  },
  {
    id: 'finance',
    title: '财务管理',
    icon: Money,
    roles: ['ADMIN', 'LEADER', 'VICE_LEADER', 'MEMBER', 'INTERN'],
    children: [
      { id: 'finance-points', path: '/points', title: '积分管理', icon: Coin, roles: ['ADMIN', 'LEADER', 'MEMBER', 'INTERN'] },
      { id: 'finance-salary', path: '/salary', title: '薪资管理', icon: TrendCharts, roles: ['ADMIN', 'LEADER', 'VICE_LEADER', 'MEMBER'] },
    ],
  },
  {
    id: 'activity',
    path: '/activities',
    title: '活动管理',
    icon: Calendar,
    roles: ['ADMIN', 'LEADER', 'MEMBER', 'INTERN'],
  },
  {
    id: 'reports',
    path: '/reports',
    title: '报表导出',
    icon: TrendCharts,
    roles: ['ADMIN', 'LEADER'],
  },
  {
    id: 'system',
    title: '系统管理',
    icon: Setting,
    roles: ['ADMIN', 'LEADER'],
    children: [
      { id: 'system-users', path: '/system/users', title: '用户管理', icon: User, roles: ['ADMIN'] },
      { id: 'system-email', path: '/emails', title: '邮件管理', icon: Message, roles: ['ADMIN', 'LEADER'] },
      { id: 'system-audit', path: '/dashboard', title: '审计日志', icon: Notebook, roles: ['ADMIN'] },
    ],
  },
]

/** 递归过滤菜单：只保留当前角色可见的节点 */
function filterMenuByRole(nodes: MenuNode[], role: string): MenuNode[] {
  return nodes
    .filter((node) => node.roles.length === 0 || node.roles.includes(role))
    .map((node) => {
      if (node.children) {
        const filtered = filterMenuByRole(node.children, role)
        // 如果子菜单全被过滤掉了，不显示父级
        if (filtered.length === 0) return null
        return { ...node, children: filtered }
      }
      return node
    })
    .filter(Boolean) as MenuNode[]
}

const visibleMenuGroups = computed(() => {
  const role = authStore.role
  if (!role) return []
  return filterMenuByRole(allMenuGroups, role)
})

const defaultOpeneds = computed(() => {
  // 根据当前路由自动展开对应的父级菜单
  const path = route.path
  const opened: string[] = []
  for (const group of allMenuGroups) {
    if (group.children) {
      for (const child of group.children) {
        if (child.path === path) {
          opened.push(group.id)
        }
        if (child.children) {
          for (const sub of child.children) {
            if (sub.path === path) {
              opened.push(group.id)
              opened.push(child.id)
            }
          }
        }
      }
    }
  }
  return opened
})

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #ffffff, #f8fafc, #ecfdf5, #f0fdf4);
}

.layout-aside {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(16, 185, 129, 0.1);
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(16, 185, 129, 0.1);
  padding: 0 16px;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #10b981;
  white-space: nowrap;
}

.logo-icon {
  font-size: 24px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

.sidebar-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 2px 8px;
  height: 42px;
  line-height: 42px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(16, 185, 129, 0.1);
}

.sidebar-menu :deep(.el-sub-menu__title) {
  border-radius: 8px;
  margin: 2px 8px;
  height: 44px;
  line-height: 44px;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 48px !important;
}

.sidebar-menu :deep(.el-sub-menu .el-sub-menu .el-menu-item) {
  padding-left: 64px !important;
}

.sidebar-menu :deep(.el-sub-menu .el-sub-menu__title) {
  padding-left: 48px !important;
}

.main-container {
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 60px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(16, 185, 129, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-avatar {
  background: #10b981;
  color: #fff;
  font-weight: 600;
}

.username {
  font-size: 14px;
  color: #374151;
}

.layout-main {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}
</style>
