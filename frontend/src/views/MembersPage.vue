<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">成员管理与流转</h2>

    <el-tabs v-model="activeTab">
      <!-- 成员列表 Tab（卡片式布局） -->
      <el-tab-pane label="成员列表" name="members">
        <div class="flex items-center gap-2 mb-4">
          <el-button @click="refreshMemberCards" :loading="loadingCards">刷新</el-button>
        </div>

        <div v-loading="loadingCards" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          <div
            v-for="member in memberCards"
            :key="member.id"
            class="member-card cursor-pointer"
            @click="openMemberDetail(member.id)"
          >
            <div class="flex items-center gap-3 mb-3">
              <div class="relative">
                <div class="w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-semibold text-lg">
                  {{ member.username.charAt(0).toUpperCase() }}
                </div>
                <span
                  class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full border-2 border-white"
                  :class="statusDotClass(member.onlineStatus)"
                  :title="statusLabel(member.onlineStatus)"
                />
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium text-gray-800 truncate">{{ member.username }}</div>
                <el-tag :type="roleTagType(member.role)" size="small" class="mt-0.5">{{ roleLabel(member.role) }}</el-tag>
              </div>
            </div>
            <div class="text-xs text-gray-500 flex items-center gap-1">
              <span class="inline-block w-2 h-2 rounded-full" :class="statusDotClass(member.onlineStatus)" />
              {{ statusLabel(member.onlineStatus) }}
            </div>
          </div>
        </div>
        <el-empty v-if="!loadingCards && memberCards.length === 0" description="暂无成员数据" />
      </el-tab-pane>

      <!-- 转正评议 Tab -->
      <el-tab-pane label="转正评议" name="promotion">
        <div class="flex items-center gap-2 mb-4">
          <el-button type="primary" @click="handleTriggerReview" :loading="triggeringReview">触发转正评议</el-button>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <h3 class="text-base font-medium text-gray-700 mb-2">符合转正条件的实习成员</h3>
            <el-table :data="eligibleInterns" v-loading="loadingPromotion" stripe size="small">
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }"><el-tag size="small">{{ row.role }}</el-tag></template>
              </el-table-column>
            </el-table>
          </div>
          <div>
            <h3 class="text-base font-medium text-gray-700 mb-2">薪酬不达标的正式成员</h3>
            <el-table :data="demotionCandidates" v-loading="loadingPromotion" stripe size="small">
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }"><el-tag size="small">{{ row.role }}</el-tag></template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        <div v-if="eligibleInterns.length > 0 && demotionCandidates.length > 0" class="mt-4 p-4 bg-gray-50 rounded-lg border">
          <h3 class="text-base font-medium text-gray-700 mb-3">执行转正流转</h3>
          <div class="flex items-end gap-4 flex-wrap">
            <div>
              <label class="block text-sm text-gray-600 mb-1">转正实习成员</label>
              <el-select v-model="promotionForm.internId" placeholder="选择实习成员" class="w-48">
                <el-option v-for="m in eligibleInterns" :key="m.id" :label="m.username" :value="m.id" />
              </el-select>
            </div>
            <div>
              <label class="block text-sm text-gray-600 mb-1">替换正式成员</label>
              <el-select v-model="promotionForm.memberId" placeholder="选择正式成员" class="w-48">
                <el-option v-for="m in demotionCandidates" :key="m.id" :label="m.username" :value="m.id" />
              </el-select>
            </div>
            <el-button type="success" @click="handleExecutePromotion" :loading="executingPromotion"
              :disabled="!promotionForm.internId || !promotionForm.memberId">执行流转</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 待开除 Tab -->
      <el-tab-pane label="待开除" name="dismissal">
        <div class="flex items-center gap-2 mb-4">
          <el-button @click="handleMarkDismissal" :loading="markingDismissal">标记待开除</el-button>
          <el-button @click="fetchPendingDismissal" :loading="loadingDismissal">刷新</el-button>
        </div>
        <el-table :data="pendingDismissalList" v-loading="loadingDismissal" stripe class="w-full">
          <el-table-column prop="username" label="用户名" width="160" />
          <el-table-column prop="role" label="角色" width="140">
            <template #default="{ row }"><el-tag size="small">{{ row.role }}</el-tag></template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default><el-tag type="danger" size="small">待开除</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Member Detail Dialog -->
    <el-dialog v-model="detailVisible" title="成员详情" width="700px" destroy-on-close>
      <div v-loading="loadingDetail">
        <template v-if="memberDetail">
          <div class="flex items-center gap-4 mb-6">
            <div class="relative">
              <div class="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold text-2xl">
                {{ memberDetail.username.charAt(0).toUpperCase() }}
              </div>
              <span class="absolute -bottom-0.5 -right-0.5 w-4 h-4 rounded-full border-2 border-white"
                :class="statusDotClass(memberDetail.onlineStatus)" />
            </div>
            <div>
              <div class="text-lg font-semibold text-gray-800">{{ memberDetail.username }}</div>
              <div class="flex items-center gap-2 mt-1">
                <el-tag :type="roleTagType(memberDetail.role)" size="small">{{ roleLabel(memberDetail.role) }}</el-tag>
                <span class="text-sm text-gray-500">{{ statusLabel(memberDetail.onlineStatus) }}</span>
              </div>
              <div class="text-xs text-gray-400 mt-1">
                加入时间：{{ formatDate(memberDetail.createdAt) }}
                <span v-if="memberDetail.lastActiveAt" class="ml-3">最后活跃：{{ formatDate(memberDetail.lastActiveAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Activity hours chart -->
          <div class="mb-6">
            <h4 class="text-sm font-medium text-gray-700 mb-3">每周活跃时长</h4>
            <div v-if="memberDetail.activityHours.length > 0" class="flex items-end gap-3 h-32">
              <div v-for="(week, idx) in memberDetail.activityHours" :key="idx" class="flex-1 flex flex-col items-center">
                <div class="text-xs text-gray-600 mb-1">{{ formatMinutes(week.totalMinutes) }}</div>
                <div class="w-full rounded-t-md transition-all"
                  :style="{ height: barHeight(week.totalMinutes) + 'px', backgroundColor: '#10b981', minHeight: '4px' }" />
                <div class="text-xs text-gray-400 mt-1 text-center leading-tight">{{ formatWeekLabel(week.weekStart) }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无活跃数据" :image-size="60" />
          </div>

          <!-- Role change history timeline -->
          <div>
            <h4 class="text-sm font-medium text-gray-700 mb-3">角色变更历史</h4>
            <div v-if="memberDetail.roleHistory.length > 0">
              <el-timeline>
                <el-timeline-item v-for="record in memberDetail.roleHistory" :key="record.id"
                  :timestamp="formatDate(record.changedAt)" placement="top" color="#10b981">
                  <div class="text-sm">
                    <el-tag :type="roleTagType(record.oldRole)" size="small">{{ roleLabel(record.oldRole) }}</el-tag>
                    <span class="mx-2 text-gray-400">→</span>
                    <el-tag :type="roleTagType(record.newRole)" size="small">{{ roleLabel(record.newRole) }}</el-tag>
                  </div>
                  <div class="text-xs text-gray-400 mt-1">操作人：{{ record.changedBy }}</div>
                </el-timeline-item>
              </el-timeline>
            </div>
            <el-empty v-else description="暂无角色变更记录" :image-size="60" />
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  checkPromotionEligibility, checkDemotionCandidates, triggerPromotionReview,
  executePromotion, markForDismissal, getPendingDismissalList,
} from '@/api/members'
import type { MemberDTO } from '@/api/members'
import { listMembers, getMemberDetail } from '@/api/member'
import type { MemberCardItem, MemberDetail, OnlineStatus } from '@/api/member'

const activeTab = ref('members')

// --- Member cards ---
const memberCards = ref<MemberCardItem[]>([])
const loadingCards = ref(false)

async function refreshMemberCards() {
  loadingCards.value = true
  try {
    const res = await listMembers()
    memberCards.value = res.data ?? []
    await refreshRotationData()
  } catch { /* interceptor */ } finally { loadingCards.value = false }
}

// --- Member detail dialog ---
const detailVisible = ref(false)
const loadingDetail = ref(false)
const memberDetail = ref<MemberDetail | null>(null)

async function openMemberDetail(id: number) {
  detailVisible.value = true
  loadingDetail.value = true
  memberDetail.value = null
  try {
    const res = await getMemberDetail(id)
    memberDetail.value = res.data
  } catch { /* interceptor */ } finally { loadingDetail.value = false }
}

// --- Helpers ---
function statusDotClass(status: OnlineStatus | string): string {
  switch (status) {
    case 'ONLINE': return 'bg-green-500'
    case 'BUSY': return 'bg-yellow-400'
    default: return 'bg-gray-300'
  }
}
function statusLabel(status: OnlineStatus | string): string {
  switch (status) {
    case 'ONLINE': return '在线'
    case 'BUSY': return '忙碌'
    default: return '离线'
  }
}
function roleTagType(role: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const m: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ADMIN: 'danger', LEADER: 'warning', VICE_LEADER: '', MEMBER: 'success', INTERN: 'info', APPLICANT: 'info',
  }
  return m[role] ?? 'info'
}
function roleLabel(role: string): string {
  const m: Record<string, string> = {
    ADMIN: '管理员', LEADER: '组长', VICE_LEADER: '副组长', MEMBER: '正式成员', INTERN: '实习成员', APPLICANT: '申请者',
  }
  return m[role] ?? role
}
function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function formatMinutes(minutes: number): string {
  if (minutes < 60) return `${minutes}分`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h}时${m}分` : `${h}时`
}
function formatWeekLabel(weekStart: string): string {
  const d = new Date(weekStart)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
function barHeight(minutes: number): number {
  const max = Math.max(...((memberDetail.value?.activityHours ?? []).map((w) => w.totalMinutes)), 1)
  return Math.max((minutes / max) * 100, 4)
}

// --- Rotation data ---
const eligibleInterns = ref<MemberDTO[]>([])
const demotionCandidates = ref<MemberDTO[]>([])
const pendingDismissalList = ref<MemberDTO[]>([])
const loadingPromotion = ref(false)
const loadingDismissal = ref(false)

async function refreshRotationData() {
  loadingPromotion.value = true
  loadingDismissal.value = true
  try {
    const [eligRes, demRes, disRes] = await Promise.all([
      checkPromotionEligibility(), checkDemotionCandidates(), getPendingDismissalList(),
    ])
    eligibleInterns.value = eligRes.data ?? []
    demotionCandidates.value = demRes.data ?? []
    pendingDismissalList.value = disRes.data ?? []
  } catch { /* interceptor */ } finally {
    loadingPromotion.value = false
    loadingDismissal.value = false
  }
}

// --- Promotion ---
const triggeringReview = ref(false)
const executingPromotion = ref(false)
const promotionForm = reactive({ internId: null as number | null, memberId: null as number | null })

async function handleTriggerReview() {
  triggeringReview.value = true
  try {
    const res = await triggerPromotionReview()
    if (res.data) ElMessage.success('转正评议已触发')
    else ElMessage.warning('当前不满足转正评议条件')
    await refreshRotationData()
  } catch { /* interceptor */ } finally { triggeringReview.value = false }
}

async function handleExecutePromotion() {
  if (!promotionForm.internId || !promotionForm.memberId) return
  try {
    await ElMessageBox.confirm('确定要执行角色流转吗？实习成员将转正，对应正式成员将降为实习。', '确认流转', { type: 'warning' })
  } catch { return }
  executingPromotion.value = true
  try {
    await executePromotion({ internId: promotionForm.internId, memberId: promotionForm.memberId })
    ElMessage.success('角色流转执行成功')
    promotionForm.internId = null
    promotionForm.memberId = null
    await refreshRotationData()
  } catch { /* interceptor */ } finally { executingPromotion.value = false }
}

// --- Dismissal ---
const markingDismissal = ref(false)

async function fetchPendingDismissal() {
  loadingDismissal.value = true
  try {
    const res = await getPendingDismissalList()
    pendingDismissalList.value = res.data ?? []
  } catch { /* interceptor */ } finally { loadingDismissal.value = false }
}

async function handleMarkDismissal() {
  markingDismissal.value = true
  try {
    const res = await markForDismissal()
    const marked = res.data ?? []
    if (marked.length > 0) ElMessage.success(`已标记 ${marked.length} 名成员为待开除`)
    else ElMessage.info('当前没有需要标记的成员')
    await fetchPendingDismissal()
  } catch { /* interceptor */ } finally { markingDismissal.value = false }
}

onMounted(() => { refreshMemberCards() })
</script>

<style scoped>
.member-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s, transform 0.2s;
}
.member-card:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.12);
  transform: translateY(-2px);
}
</style>
