<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">积分管理</h2>

    <!-- Stats overview -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <div class="stats-card bg-white/80 rounded-xl border border-gray-100 shadow-sm p-4">
        <div class="text-sm text-gray-500">查询用户ID</div>
        <div class="text-2xl font-bold text-emerald-600">{{ queryUserId ?? '-' }}</div>
      </div>
      <div class="stats-card bg-white/80 rounded-xl border border-gray-100 shadow-sm p-4">
        <div class="text-sm text-gray-500">总积分</div>
        <div class="text-2xl font-bold text-emerald-600">{{ totalPoints }}</div>
      </div>
      <div class="stats-card bg-white/80 rounded-xl border border-gray-100 shadow-sm p-4">
        <div class="text-sm text-gray-500">记录数</div>
        <div class="text-2xl font-bold text-emerald-600">{{ filteredRecords.length }}</div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
      <div class="flex items-center gap-2">
        <el-input
          v-model.number="queryUserId"
          placeholder="输入用户ID查询"
          style="width: 180px"
          @keyup.enter="fetchRecords"
        >
          <template #append>
            <el-button @click="fetchRecords">查询</el-button>
          </template>
        </el-input>
        <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 160px">
          <el-option v-for="t in pointsTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </div>
      <div v-if="canManagePoints">
        <el-button type="primary" @click="openDialog('add')">增加积分</el-button>
        <el-button type="danger" @click="openDialog('deduct')">扣减积分</el-button>
      </div>
    </div>

    <!-- Records table -->
    <el-table :data="filteredRecords" v-loading="loading" stripe class="w-full">
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column label="类型" width="140">
        <template #default="{ row }">
          {{ typeLabel(row.pointsType) }}
        </template>
      </el-table-column>
      <el-table-column label="数额" width="100">
        <template #default="{ row }">
          <span :class="row.amount >= 0 ? 'text-emerald-600' : 'text-red-500'">
            {{ row.amount >= 0 ? '+' : '' }}{{ row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Deduct dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '增加积分' : '扣减积分'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户ID">
          <el-input-number v-model="form.userId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="积分类型">
          <el-select v-model="form.pointsType" placeholder="选择类型" style="width: 100%">
            <el-option v-for="t in pointsTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数额">
          <el-input-number v-model="form.amount" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPoints">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getPointsRecords, getTotalPoints, addPoints, deductPoints } from '@/api/points'
import type { PointsRecordDTO } from '@/api/points'

const authStore = useAuthStore()

const pointsTypes = [
  { value: 'COMMUNITY_ACTIVITY', label: '社群活跃度' },
  { value: 'CHECKIN', label: '签到奖惩' },
  { value: 'VIOLATION_HANDLING', label: '处理违规' },
  { value: 'TASK_COMPLETION', label: '完成任务' },
  { value: 'ANNOUNCEMENT', label: '发布公告' },
  { value: 'EVENT_HOSTING', label: '举办活动' },
  { value: 'BIRTHDAY_BONUS', label: '生日福利' },
  { value: 'MONTHLY_EXCELLENT', label: '月度优秀' },
]

const typeMap = Object.fromEntries(pointsTypes.map((t) => [t.value, t.label]))

function typeLabel(type: string) {
  return typeMap[type] ?? type
}

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

const currentUserId = computed(() => authStore.user?.id)
const canManagePoints = computed(() => ['ADMIN', 'LEADER'].includes(authStore.role))

const queryUserId = ref<number | undefined>(undefined)
const filterType = ref('')
const records = ref<PointsRecordDTO[]>([])
const totalPoints = ref(0)
const loading = ref(false)

const filteredRecords = computed(() => {
  if (!filterType.value) return records.value
  return records.value.filter((r) => r.pointsType === filterType.value)
})

async function fetchRecords() {
  const uid = queryUserId.value ?? currentUserId.value
  if (!uid) return
  loading.value = true
  try {
    const [recRes, totalRes] = await Promise.all([getPointsRecords(uid), getTotalPoints(uid)])
    records.value = recRes.data ?? []
    totalPoints.value = totalRes.data ?? 0
  } catch {
    records.value = []
    totalPoints.value = 0
  } finally {
    loading.value = false
  }
}

// Dialog state
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'deduct'>('add')
const submitting = ref(false)
const form = ref({ userId: 1, pointsType: '', amount: 1, description: '' })

function openDialog(mode: 'add' | 'deduct') {
  dialogMode.value = mode
  form.value = { userId: queryUserId.value ?? 1, pointsType: '', amount: 1, description: '' }
  dialogVisible.value = true
}

async function submitPoints() {
  if (!form.value.pointsType) {
    ElMessage.warning('请选择积分类型')
    return
  }
  submitting.value = true
  try {
    const payload = {
      userId: form.value.userId,
      pointsType: form.value.pointsType,
      amount: form.value.amount,
      description: form.value.description,
    }
    if (dialogMode.value === 'add') {
      await addPoints(payload)
      ElMessage.success('积分增加成功')
    } else {
      await deductPoints(payload)
      ElMessage.success('积分扣减成功')
    }
    dialogVisible.value = false
    // Refresh if viewing the same user
    if (queryUserId.value === form.value.userId || (!queryUserId.value && currentUserId.value === form.value.userId)) {
      await fetchRecords()
    }
  } catch {
    // Error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (currentUserId.value) {
    queryUserId.value = currentUserId.value
    fetchRecords()
  }
})
</script>
