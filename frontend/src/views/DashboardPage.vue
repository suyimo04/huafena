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

    <!-- Recruitment Data Section -->
    <el-card class="dashboard-card mb-6" v-loading="recruitmentLoading">
      <template #header>
        <span class="text-lg font-semibold text-gray-700">招募数据</span>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Stage counts as colored bars -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">各阶段人数</h4>
          <div class="space-y-2">
            <div v-for="stage in recruitmentStages" :key="stage.key" class="flex items-center gap-2">
              <span class="text-xs text-gray-500 w-20 shrink-0 text-right">{{ stage.label }}</span>
              <div class="flex-1 h-6 bg-gray-100 rounded overflow-hidden">
                <div
                  class="h-full rounded transition-all duration-500"
                  :style="{ width: stageBarWidth(stage.key), backgroundColor: stage.color }"
                />
              </div>
              <span class="text-sm font-medium text-gray-700 w-8 text-right">{{ recruitmentStats.stageCount[stage.key] ?? 0 }}</span>
            </div>
          </div>
        </div>
        <!-- Pass rates -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">通过率</h4>
          <div class="flex gap-6">
            <div class="flex-1 text-center">
              <el-progress
                type="circle"
                :percentage="Math.round(recruitmentStats.aiInterviewPassRate * 100)"
                :width="100"
                :stroke-width="8"
                color="#10b981"
              />
              <div class="text-xs text-gray-500 mt-2">AI 面试通过率</div>
            </div>
            <div class="flex-1 text-center">
              <el-progress
                type="circle"
                :percentage="Math.round(recruitmentStats.manualReviewPassRate * 100)"
                :width="100"
                :stroke-width="8"
                color="#3b82f6"
              />
              <div class="text-xs text-gray-500 mt-2">人工复审通过率</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Salary Data Section -->
    <el-card class="dashboard-card mb-6" v-loading="salaryLoading">
      <template #header>
        <span class="text-lg font-semibold text-gray-700">薪酬数据</span>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Pool usage progress -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">薪酬池使用情况</h4>
          <div class="mb-2 flex justify-between text-sm text-gray-500">
            <span>已分配: {{ salaryStats.allocated }} 迷你币</span>
            <span>总额: {{ salaryStats.totalPool }} 迷你币</span>
          </div>
          <el-progress
            :percentage="Math.round(salaryStats.usageRate * 100)"
            :stroke-width="20"
            :text-inside="true"
            color="#10b981"
          />
        </div>
        <!-- Salary ranking table -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">成员薪酬排行榜</h4>
          <el-table :data="salaryStats.ranking" size="small" stripe max-height="220">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="username" label="成员" min-width="80" />
            <el-table-column prop="totalPoints" label="总积分" width="80" align="right" />
            <el-table-column prop="miniCoins" label="迷你币" width="80" align="right" />
          </el-table>
        </div>
      </div>
    </el-card>

    <!-- Operations Data Section -->
    <el-card class="dashboard-card mb-6" v-loading="operationsLoading">
      <template #header>
        <span class="text-lg font-semibold text-gray-700">运营数据</span>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- User growth trend bar chart (CSS-based) -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">用户增长趋势（近12个月）</h4>
          <div class="growth-chart">
            <div
              v-for="item in operationsData.userGrowthTrend"
              :key="item.month"
              class="growth-bar-wrapper"
              :title="`${item.month}: ${item.count} 人`"
            >
              <div class="growth-bar-value text-xs text-gray-500">{{ item.count || '' }}</div>
              <div class="growth-bar-track">
                <div
                  class="growth-bar"
                  :style="{ height: growthBarHeight(item.count) }"
                />
              </div>
              <div class="growth-bar-label">{{ item.month.slice(5) }}</div>
            </div>
          </div>
        </div>
        <!-- Processing efficiency stats -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">处理效率统计</h4>
          <div class="space-y-4">
            <div class="stat-card">
              <div class="stat-label">申请总数</div>
              <div class="stat-value text-lg">{{ operationsData.issueProcessingStats.totalApplications ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">已处理</div>
              <div class="stat-value text-lg">{{ operationsData.issueProcessingStats.processedApplications ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">待处理</div>
              <div class="stat-value text-lg">{{ operationsData.issueProcessingStats.pendingApplications ?? 0 }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-500 mb-1">处理率</div>
              <el-progress
                :percentage="Math.round((operationsData.issueProcessingStats.processingRate ?? 0) * 100)"
                :stroke-width="12"
                color="#10b981"
              />
            </div>
          </div>
        </div>
      </div>
    </el-card>

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
import { ref, reactive, onMounted, computed } from 'vue'
import {
  getDashboardStats,
  getAuditLogs,
  getRecruitmentStats,
  getSalaryStats,
  getOperationsData,
} from '@/api/dashboard'
import type {
  DashboardStatsDTO,
  AuditLogDTO,
  RecruitmentStatsDTO,
  SalaryStatsDTO,
  OperationsDataDTO,
} from '@/api/dashboard'

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
const recruitmentLoading = ref(false)
const salaryLoading = ref(false)
const operationsLoading = ref(false)

const auditLogs = ref<AuditLogDTO[]>([])
const filterType = ref('')

const recruitmentStats = reactive<RecruitmentStatsDTO>({
  stageCount: {},
  aiInterviewPassRate: 0,
  manualReviewPassRate: 0,
})

const salaryStats = reactive<SalaryStatsDTO>({
  totalPool: 2000,
  allocated: 0,
  usageRate: 0,
  ranking: [],
})

const operationsData = reactive<OperationsDataDTO>({
  userGrowthTrend: [],
  issueProcessingStats: {},
})

const recruitmentStages = [
  { key: 'PENDING_INITIAL_REVIEW', label: '待初审', color: '#f59e0b' },
  { key: 'INITIAL_REVIEW_PASSED', label: '初审通过', color: '#3b82f6' },
  { key: 'AI_INTERVIEW_IN_PROGRESS', label: 'AI面试中', color: '#8b5cf6' },
  { key: 'PENDING_REVIEW', label: '待复审', color: '#ec4899' },
  { key: 'HIRED', label: '已录用', color: '#10b981' },
  { key: 'REJECTED', label: '已拒绝', color: '#ef4444' },
]

const operationTypes = [
  'SALARY_SAVE',
  'ROLE_CHANGE',
  'APPLICATION_REVIEW',
  'INTERVIEW_REVIEW',
  'ACTIVITY_CREATE',
  'ACTIVITY_ARCHIVE',
  'POINTS_CHANGE',
]

const maxStageCount = computed(() => {
  const counts = Object.values(recruitmentStats.stageCount)
  return counts.length ? Math.max(...counts, 1) : 1
})

const maxGrowthCount = computed(() => {
  const counts = operationsData.userGrowthTrend.map((i) => i.count)
  return counts.length ? Math.max(...counts, 1) : 1
})

function stageBarWidth(key: string): string {
  const count = recruitmentStats.stageCount[key] ?? 0
  return `${Math.round((count / maxStageCount.value) * 100)}%`
}

function growthBarHeight(count: number): string {
  return `${Math.round((count / maxGrowthCount.value) * 100)}%`
}

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

async function fetchRecruitmentStats() {
  recruitmentLoading.value = true
  try {
    const res = await getRecruitmentStats()
    Object.assign(recruitmentStats, res.data)
  } catch {
    // handled by axios interceptor
  } finally {
    recruitmentLoading.value = false
  }
}

async function fetchSalaryStats() {
  salaryLoading.value = true
  try {
    const res = await getSalaryStats()
    Object.assign(salaryStats, res.data)
  } catch {
    // handled by axios interceptor
  } finally {
    salaryLoading.value = false
  }
}

async function fetchOperationsData() {
  operationsLoading.value = true
  try {
    const res = await getOperationsData()
    Object.assign(operationsData, res.data)
  } catch {
    // handled by axios interceptor
  } finally {
    operationsLoading.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchAuditLogs()
  fetchRecruitmentStats()
  fetchSalaryStats()
  fetchOperationsData()
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
.dashboard-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

/* CSS-based bar chart for growth trend */
.growth-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 160px;
  padding-top: 20px;
}
.growth-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}
.growth-bar-value {
  height: 18px;
  line-height: 18px;
  text-align: center;
}
.growth-bar-track {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
}
.growth-bar {
  width: 100%;
  min-height: 2px;
  background: #10b981;
  border-radius: 4px 4px 0 0;
  transition: height 0.5s ease;
}
.growth-bar-label {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 4px;
  text-align: center;
}
</style>
