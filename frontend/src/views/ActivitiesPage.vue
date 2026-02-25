<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">活动管理</h2>

    <!-- Toolbar -->
    <div class="flex items-center justify-between mb-4">
      <div class="text-sm text-gray-500">共 {{ activities.length }} 个活动</div>
      <el-button v-if="canManage" type="primary" @click="openCreateDialog">创建活动</el-button>
    </div>

    <!-- Activity table -->
    <el-table :data="activities" v-loading="loading" stripe class="w-full">
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地点" width="140" />
      <el-table-column prop="registrationCount" label="报名人数" width="100" align="center" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center">
        <template #default="{ row }">
          <el-button
            v-if="!canManage && row.status !== 'ARCHIVED'"
            size="small"
            type="primary"
            @click="handleRegister(row.id)"
          >报名</el-button>
          <el-button
            v-if="!canManage && row.status !== 'ARCHIVED'"
            size="small"
            type="success"
            @click="handleCheckIn(row.id)"
          >签到</el-button>
          <el-button
            v-if="canManage && row.status !== 'ARCHIVED'"
            size="small"
            type="warning"
            @click="handleArchive(row.id)"
          >归档</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create activity dialog -->
    <el-dialog v-model="createDialogVisible" title="创建活动" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入活动描述" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="form.eventTime"
            type="datetime"
            placeholder="选择活动时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="请输入活动地点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  listActivities,
  createActivity,
  registerForActivity,
  checkInActivity,
  archiveActivity,
} from '@/api/activity'
import type { ActivityDTO } from '@/api/activity'

const authStore = useAuthStore()

const canManage = computed(() => ['ADMIN', 'LEADER'].includes(authStore.role))
const currentUserId = computed(() => authStore.user?.id)

const activities = ref<ActivityDTO[]>([])
const loading = ref(false)

const statusMap: Record<string, string> = {
  UPCOMING: '即将开始',
  ONGOING: '进行中',
  COMPLETED: '已完成',
  ARCHIVED: '已归档',
}

const statusTagTypeMap: Record<string, string> = {
  UPCOMING: 'info',
  ONGOING: 'success',
  COMPLETED: 'warning',
  ARCHIVED: '',
}

function statusLabel(status: string) {
  return statusMap[status] ?? status
}

function statusTagType(status: string) {
  return statusTagTypeMap[status] ?? 'info'
}

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function fetchActivities() {
  loading.value = true
  try {
    const res = await listActivities()
    activities.value = res.data ?? []
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

// Create dialog
const createDialogVisible = ref(false)
const submitting = ref(false)
const form = ref({ name: '', description: '', eventTime: '', location: '' })

function openCreateDialog() {
  form.value = { name: '', description: '', eventTime: '', location: '' }
  createDialogVisible.value = true
}

async function submitCreate() {
  if (!form.value.name) {
    ElMessage.warning('请输入活动名称')
    return
  }
  if (!form.value.eventTime) {
    ElMessage.warning('请选择活动时间')
    return
  }
  if (!form.value.location) {
    ElMessage.warning('请输入活动地点')
    return
  }
  submitting.value = true
  try {
    const eventTime = typeof form.value.eventTime === 'string'
      ? form.value.eventTime
      : new Date(form.value.eventTime).toISOString().substring(0, 19)
    await createActivity({
      name: form.value.name,
      description: form.value.description || undefined,
      eventTime,
      location: form.value.location,
      createdBy: currentUserId.value!,
    })
    ElMessage.success('活动创建成功')
    createDialogVisible.value = false
    await fetchActivities()
  } catch {
    // handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

async function handleRegister(activityId: number) {
  if (!currentUserId.value) return
  try {
    await registerForActivity(activityId, currentUserId.value)
    ElMessage.success('报名成功')
    await fetchActivities()
  } catch {
    // handled by axios interceptor
  }
}

async function handleCheckIn(activityId: number) {
  if (!currentUserId.value) return
  try {
    await checkInActivity(activityId, currentUserId.value)
    ElMessage.success('签到成功')
    await fetchActivities()
  } catch {
    // handled by axios interceptor
  }
}

async function handleArchive(activityId: number) {
  try {
    await archiveActivity(activityId)
    ElMessage.success('活动已归档')
    await fetchActivities()
  } catch {
    // handled by axios interceptor
  }
}

onMounted(() => {
  fetchActivities()
})
</script>
