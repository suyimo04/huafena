<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">数据看板</h2>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6" v-loading="statsLoading">
      <div class="stat-card">
        <div class="stat-label">成员总数</div>
        <div class="stat-value">{{ stats.totalMembers }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">管理员</div>
        <div class="stat-value">{{ stats.adminCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">组长</div>
        <div class="stat-value">{{ stats.leaderCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">副组长</div>
        <div class="stat-value">{{ stats.viceLeaderCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">正式成员</div>
        <div class="stat-value">{{ stats.memberCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">实习成员</div>
        <div class="stat-value">{{ stats.internCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">申请者</div>
        <div class="stat-value">{{ stats.applicantCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">活动总数</div>
        <div class="stat-value">{{ stats.totalActivities }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">积分记录</div>
        <div class="stat-value">{{ stats.totalPointsRecords }}</div>
      </div>
    </div>

    <!-- Audit Logs -->
    <h3 class="text-lg font-semibold text-gray-700 mb-3">审计日志</h3>

    <div class="flex items-center mb-3">
      <el-select
        v-model="filterType"
        placeholder="按操作类型筛选"
        clearable
        style="width: 220px"
        @change="fetchAuditLogs"
      >
        <el-option
          v-for="t in operationTypes"
          :key="t"
          :label="t"
          :value="t"
        />
      </el-select>
    </div>

    <el-table :data="auditLogs" v-loading="logsLoading" stripe class="w-full">
      <el-table-column prop="operatorId" label="操作人ID" width="140" />
      <el-table-column prop="operationType" label="操作类型" width="160" />
      <el-table-column label="操作时间" width="180">
        <template #default="{ row }">{{ formatTime(row.operationTime) }}</template>
      </el-table-column>
      <el-table-column prop="operationDetail" label="操作详情" min-width="240" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDashboardStats, getAuditLogs } from '@/api/dashboard'
import type { DashboardStatsDTO, AuditLogDTO } from '@/api/dashboard'

const stats = reactive<DashboardStatsDTO>({
  totalMembers: 0,
  adminCount: 0,
  leaderCount: 0,
  viceLeaderCount: 0,
  memberCount: 0,
  internCount: 0,
  applicantCount: 0,
  totalActivities: 0,
  totalPointsRecords: 0,
})

const statsLoading = ref(false)
const logsLoading = ref(false)
const auditLogs = ref<AuditLogDTO[]>([])
const filterType = ref('')

const operationTypes = [
  'SALARY_SAVE',
  'ROLE_CHANGE',
  'APPLICATION_REVIEW',
  'INTERVIEW_REVIEW',
  'ACTIVITY_CREATE',
  'ACTIVITY_ARCHIVE',
  'POINTS_CHANGE',
]

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function fetchStats() {
  statsLoading.value = true
  try {
    const res = await getDashboardStats()
    Object.assign(stats, res.data)
  } catch {
    // handled by axios interceptor
  } finally {
    statsLoading.value = false
  }
}

async function fetchAuditLogs() {
  logsLoading.value = true
  try {
    const res = await getAuditLogs(filterType.value || undefined)
    auditLogs.value = res.data ?? []
  } catch {
    auditLogs.value = []
  } finally {
    logsLoading.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchAuditLogs()
})
</script>

<style scoped>
.stat-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #10b981;
}
</style>
