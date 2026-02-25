<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">成员管理与流转</h2>

    <el-tabs v-model="activeTab">
      <!-- 成员列表 Tab -->
      <el-tab-pane label="成员列表" name="members">
        <div class="flex items-center gap-2 mb-4">
          <el-button @click="refreshMemberList" :loading="loadingMembers">刷新</el-button>
        </div>
        <el-table :data="allMembers" v-loading="loadingMembers" stripe class="w-full">
          <el-table-column prop="username" label="用户名" width="160" />
          <el-table-column prop="role" label="角色" width="140">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)" size="small">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.pendingDismissal" type="danger" size="small">待开除</el-tag>
              <el-tag v-else-if="row.enabled" type="success" size="small">正常</el-tag>
              <el-tag v-else type="info" size="small">禁用</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="160">
            <template #default="{ row }">
              <span v-if="isEligibleIntern(row.id)" class="text-green-600 text-sm">符合转正条件</span>
              <span v-else-if="isDemotionCandidate(row.id)" class="text-red-500 text-sm">薪酬不达标</span>
              <span v-else class="text-gray-400 text-sm">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 转正评议 Tab -->
      <el-tab-pane label="转正评议" name="promotion">
        <div class="flex items-center gap-2 mb-4">
          <el-button type="primary" @click="handleTriggerReview" :loading="triggeringReview">
            触发转正评议
          </el-button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <!-- Eligible interns -->
          <div>
            <h3 class="text-base font-medium text-gray-700 mb-2">符合转正条件的实习成员</h3>
            <el-table :data="eligibleInterns" v-loading="loadingPromotion" stripe size="small">
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- Demotion candidates -->
          <div>
            <h3 class="text-base font-medium text-gray-700 mb-2">薪酬不达标的正式成员</h3>
            <el-table :data="demotionCandidates" v-loading="loadingPromotion" stripe size="small">
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- Execute promotion form -->
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
              :disabled="!promotionForm.internId || !promotionForm.memberId">
              执行流转
            </el-button>
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
            <template #default="{ row }">
              <el-tag size="small">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default>
              <el-tag type="danger" size="small">待开除</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  checkPromotionEligibility,
  checkDemotionCandidates,
  triggerPromotionReview,
  executePromotion,
  markForDismissal,
  getPendingDismissalList,
} from '@/api/members'
import type { MemberDTO } from '@/api/members'

const activeTab = ref('members')

// --- Member list ---
const eligibleInterns = ref<MemberDTO[]>([])
const demotionCandidates = ref<MemberDTO[]>([])
const loadingMembers = ref(false)
const loadingPromotion = ref(false)

const allMembers = computed(() => {
  const map = new Map<number, MemberDTO>()
  for (const m of eligibleInterns.value) map.set(m.id, m)
  for (const m of demotionCandidates.value) map.set(m.id, m)
  for (const m of pendingDismissalList.value) map.set(m.id, m)
  return Array.from(map.values())
})

function isEligibleIntern(id: number): boolean {
  return eligibleInterns.value.some((m) => m.id === id)
}

function isDemotionCandidate(id: number): boolean {
  return demotionCandidates.value.some((m) => m.id === id)
}

function roleTagType(role: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ADMIN: 'danger',
    LEADER: 'warning',
    VICE_LEADER: '',
    MEMBER: 'success',
    INTERN: 'info',
    APPLICANT: 'info',
  }
  return map[role] ?? 'info'
}

async function refreshMemberList() {
  loadingMembers.value = true
  try {
    const [eligRes, demRes, disRes] = await Promise.all([
      checkPromotionEligibility(),
      checkDemotionCandidates(),
      getPendingDismissalList(),
    ])
    eligibleInterns.value = eligRes.data ?? []
    demotionCandidates.value = demRes.data ?? []
    pendingDismissalList.value = disRes.data ?? []
  } catch {
    // handled by interceptor
  } finally {
    loadingMembers.value = false
  }
}

// --- Promotion ---
const triggeringReview = ref(false)
const executingPromotion = ref(false)
const promotionForm = reactive({ internId: null as number | null, memberId: null as number | null })

async function fetchPromotionData() {
  loadingPromotion.value = true
  try {
    const [eligRes, demRes] = await Promise.all([
      checkPromotionEligibility(),
      checkDemotionCandidates(),
    ])
    eligibleInterns.value = eligRes.data ?? []
    demotionCandidates.value = demRes.data ?? []
  } catch {
    // handled by interceptor
  } finally {
    loadingPromotion.value = false
  }
}

async function handleTriggerReview() {
  triggeringReview.value = true
  try {
    const res = await triggerPromotionReview()
    if (res.data) {
      ElMessage.success('转正评议已触发')
    } else {
      ElMessage.warning('当前不满足转正评议条件')
    }
    await fetchPromotionData()
  } catch {
    // handled by interceptor
  } finally {
    triggeringReview.value = false
  }
}

async function handleExecutePromotion() {
  if (!promotionForm.internId || !promotionForm.memberId) return
  try {
    await ElMessageBox.confirm(
      '确定要执行角色流转吗？实习成员将转正，对应正式成员将降为实习。',
      '确认流转',
      { type: 'warning' },
    )
  } catch {
    return
  }
  executingPromotion.value = true
  try {
    await executePromotion({
      internId: promotionForm.internId,
      memberId: promotionForm.memberId,
    })
    ElMessage.success('角色流转执行成功')
    promotionForm.internId = null
    promotionForm.memberId = null
    await fetchPromotionData()
  } catch {
    // handled by interceptor
  } finally {
    executingPromotion.value = false
  }
}

// --- Pending dismissal ---
const pendingDismissalList = ref<MemberDTO[]>([])
const loadingDismissal = ref(false)
const markingDismissal = ref(false)

async function fetchPendingDismissal() {
  loadingDismissal.value = true
  try {
    const res = await getPendingDismissalList()
    pendingDismissalList.value = res.data ?? []
  } catch {
    // handled by interceptor
  } finally {
    loadingDismissal.value = false
  }
}

async function handleMarkDismissal() {
  markingDismissal.value = true
  try {
    const res = await markForDismissal()
    const marked = res.data ?? []
    if (marked.length > 0) {
      ElMessage.success(`已标记 ${marked.length} 名成员为待开除`)
    } else {
      ElMessage.info('当前没有需要标记的成员')
    }
    await fetchPendingDismissal()
  } catch {
    // handled by interceptor
  } finally {
    markingDismissal.value = false
  }
}

onMounted(() => {
  refreshMemberList()
})
</script>
