<template>
  <div class="dashboard-page p-6">
    <h2 class="page-title">数据看板</h2>

    <!-- Top Stats Cards - Art Design Pro style -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-5 mb-6" v-loading="statsLoading">
      <div class="stat-card" v-for="card in topCards" :key="card.label">
        <div class="stat-card-inner">
          <div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-number">{{ card.value }}</div>
            <div class="stat-sub" v-if="card.sub">{{ card.sub }}</div>
          </div>
          <div class="stat-icon" :class="card.iconClass">
            <component :is="card.icon" style="width:22px;height:22px" />
          </div>
        </div>
      </div>
    </div>

    <!-- Row 2: Recruitment + Salary -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-6">
      <!-- Recruitment Card -->
      <div class="panel-card" v-loading="recruitmentLoading">
        <div class="panel-header">
          <span class="panel-title">招募数据</span>
        </div>
        <div class="panel-body">
          <div class="space-y-3">
            <div v-for="stage in recruitmentStages" :key="stage.key" class="stage-row">
              <span class="stage-label">{{ stage.label }}</span>
              <div class="stage-track">
                <div class="stage-bar" :style="{ width: stageBarWidth(stage.key), backgroundColor: stage.color }" />
              </div>
              <span class="stage-count">{{ recruitmentStats.stageCount[stage.key] ?? 0 }}</span>
            </div>
          </div>
          <div class="flex gap-8 mt-6 justify-center">
            <div class="text-center">
              <el-progress type="circle" :percentage="Math.round(recruitmentStats.aiInterviewPassRate * 100)" :width="90" :stroke-width="7" color="#e91e63" />
              <div class="progress-label">AI 面试通过率</div>
            </div>
            <div class="text-center">
              <el-progress type="circle" :percentage="Math.round(recruitmentStats.manualReviewPassRate * 100)" :width="90" :stroke-width="7" color="#42a5f5" />
              <div class="progress-label">人工复审通过率</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Salary Card -->
      <div class="panel-card" v-loading="salaryLoading">
        <div class="panel-header">
          <span class="panel-title">薪酬数据</span>
        </div>
        <div class="panel-body">
          <div class="mb-4">
            <div class="flex justify-between text-sm text-gray-500 mb-2">
              <span>已分配: {{ salaryStats.allocated }} 迷你币</span>
              <span>总额: {{ salaryStats.totalPool }} 迷你币</span>
            </div>
            <el-progress :percentage="Math.round(salaryStats.usageRate * 100)" :stroke-width="16" :text-inside="true" color="#e91e63" />
          </div>
          <div class="ranking-title">成员薪酬排行榜</div>
          <el-table :data="salaryStats.ranking" size="small" max-height="200" class="ranking-table">
            <el-table-column type="index" label="#" width="45" align="center" />
            <el-table-column prop="username" label="成员" min-width="80" />
            <el-table-column prop="totalPoints" label="总积分" width="75" align="right" />
            <el-table-column prop="miniCoins" label="迷你币" width="75" align="right" />
          </el-table>
        </div>
      </div>
    </div>

    <!-- Row 3: Growth Trend + Processing Stats -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-6">
      <!-- Growth Trend -->
      <div class="panel-card" v-loading="operationsLoading">
        <div class="panel-header">
          <span class="panel-title">用户增长趋势</span>
          <span class="panel-sub">近12个月</span>
        </div>
        <div class="panel-body">
          <div class="growth-chart">
            <div v-for="item in operationsData.userGrowthTrend" :key="item.month" class="growth-col" :title="`${item.month}: ${item.count} 人`">
              <div class="growth-val">{{ item.count || '' }}</div>
              <div class="growth-track">
                <div class="growth-bar" :style="{ height: growthBarHeight(item.count) }" />
              </div>
              <div class="growth-label">{{ item.month.slice(5) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Processing Stats -->
      <div class="panel-card" v-loading="operationsLoading">
        <div class="panel-header">
          <span class="panel-title">处理效率统计</span>
        </div>
        <div class="panel-body">
          <div class="grid grid-cols-3 gap-4 mb-5">
            <div class="mini-stat">
              <div class="mini-stat-num">{{ operationsData.issueProcessingStats.totalApplications ?? 0 }}</div>
              <div class="mini-stat-label">申请总数</div>
            </div>
            <div class="mini-stat">
              <div class="mini-stat-num">{{ operationsData.issueProcessingStats.processedApplications ?? 0 }}</div>
              <div class="mini-stat-label">已处理</div>
            </div>
            <div class="mini-stat">
              <div class="mini-stat-num">{{ operationsData.issueProcessingStats.pendingApplications ?? 0 }}</div>
              <div class="mini-stat-label">待处理</div>
            </div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-2">处理率</div>
            <el-progress :percentage="Math.round((operationsData.issueProcessingStats.processingRate ?? 0) * 100)" :stroke-width="14" color="#e91e63" />
          </div>
        </div>
      </div>
    </div>

    <!-- Audit Logs -->
    <div class="panel-card">
      <div class="panel-header">
        <span class="panel-title">审计日志</span>
        <el-select v-model="filterType" placeholder="按操作类型筛选" clearable size="small" style="width: 200px" @change="fetchAuditLogs">
          <el-option v-for="t in operationTypes" :key="t" :label="t" :value="t" />
        </el-select>
      </div>
      <div class="panel-body" v-loading="logsLoading">
        <el-table :data="auditLogs" size="small" class="ranking-table">
          <el-table-column prop="operatorId" label="操作人ID" width="120" />
          <el-table-column prop="operationType" label="操作类型" width="160" />
          <el-table-column label="操作时间" width="180">
            <template #default="{ row }">{{ formatTime(row.operationTime) }}</template>
          </el-table-column>
          <el-table-column prop="operationDetail" label="操作详情" min-width="240" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { User, DataLine, Coin, TrendCharts } from '@element-plus/icons-vue'
import {
  getDashboardStats,
  getAuditLogs,
  getRecruitmentStats,
  getSalaryStats,
  getOperationsData,
  type DashboardStatsDTO,
  type AuditLogDTO,
  type RecruitmentStatsDTO,
  type SalaryStatsDTO,
  type OperationsDataDTO,
} from '@/api/dashboard'

const statsLoading = ref(false)
const recruitmentLoading = ref(false)
const salaryLoading = ref(false)
const operationsLoading = ref(false)
const logsLoading = ref(false)

const stats = ref<DashboardStatsDTO>({
  totalMembers: 0, adminCount: 0, leaderCount: 0, viceLeaderCount: 0,
  memberCount: 0, internCount: 0, applicantCount: 0, totalActivities: 0, totalPointsRecords: 0,
})

const recruitmentStats = ref<RecruitmentStatsDTO>({
  stageCount: {}, aiInterviewPassRate: 0, manualReviewPassRate: 0,
})

const salaryStats = ref<SalaryStatsDTO>({
  totalPool: 0, allocated: 0, usageRate: 0, ranking: [],
})

const operationsData = ref<OperationsDataDTO>({
  userGrowthTrend: [], issueProcessingStats: {},
})

const auditLogs = ref<AuditLogDTO[]>([])
const filterType = ref('')

const operationTypes = [
  'MEMBER_CREATE', 'MEMBER_UPDATE', 'MEMBER_DELETE',
  'APPLICATION_CREATE', 'APPLICATION_UPDATE',
  'POINTS_AWARD', 'POINTS_DEDUCT',
  'SALARY_CALCULATE', 'SALARY_ARCHIVE',
]

const topCards = computed(() => [
  { label: '总成员', value: stats.value.totalMembers, sub: `含实习 ${stats.value.internCount}`, icon: User, iconClass: 'pink-bg' },
  { label: '总活动', value: stats.value.totalActivities, sub: '', icon: DataLine, iconClass: 'blue-bg' },
  { label: '积分记录', value: stats.value.totalPointsRecords, sub: '', icon: Coin, iconClass: 'orange-bg' },
  { label: '申请人数', value: stats.value.applicantCount, sub: '', icon: TrendCharts, iconClass: 'green-bg' },
])

const recruitmentStages = [
  { key: 'PENDING', label: '待审核', color: '#90caf9' },
  { key: 'AI_INTERVIEW', label: 'AI 面试', color: '#ce93d8' },
  { key: 'MANUAL_REVIEW', label: '人工复审', color: '#f48fb1' },
  { key: 'APPROVED', label: '已通过', color: '#a5d6a7' },
  { key: 'REJECTED', label: '已拒绝', color: '#ef9a9a' },
]

function stageBarWidth(key: string): string {
  const count = recruitmentStats.value.stageCount[key] ?? 0
  const max = Math.max(...Object.values(recruitmentStats.value.stageCount), 1)
  return `${Math.round((count / max) * 100)}%`
}

function growthBarHeight(count: number): string {
  const max = Math.max(...operationsData.value.userGrowthTrend.map(i => i.count), 1)
  return `${Math.round((count / max) * 100)}%`
}

function formatTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function fetchStats() {
  statsLoading.value = true
  try {
    const res = await getDashboardStats()
    if (res.data) stats.value = res.data
  } catch { /* handled */ } finally { statsLoading.value = false }
}

async function fetchRecruitmentStats() {
  recruitmentLoading.value = true
  try {
    const res = await getRecruitmentStats()
    if (res.data) recruitmentStats.value = res.data
  } catch { /* handled */ } finally { recruitmentLoading.value = false }
}

async function fetchSalaryStats() {
  salaryLoading.value = true
  try {
    const res = await getSalaryStats()
    if (res.data) salaryStats.value = res.data
  } catch { /* handled */ } finally { salaryLoading.value = false }
}

async function fetchOperationsData() {
  operationsLoading.value = true
  try {
    const res = await getOperationsData()
    if (res.data) operationsData.value = res.data
  } catch { /* handled */ } finally { operationsLoading.value = false }
}

async function fetchAuditLogs() {
  logsLoading.value = true
  try {
    const res = await getAuditLogs(filterType.value || undefined)
    if (res.data) auditLogs.value = res.data
  } catch { /* handled */ } finally { logsLoading.value = false }
}

onMounted(() => {
  fetchStats()
  fetchRecruitmentStats()
  fetchSalaryStats()
  fetchOperationsData()
  fetchAuditLogs()
})
</script>

<style scoped>
/* ===== Art Design Pro - Pink Theme ===== */
.dashboard-page { background: #fafafa; min-height: 100%; }
.page-title { font-size: 20px; font-weight: 600; color: #333; margin-bottom: 20px; }

/* --- Stat Cards --- */
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 2px 12px rgba(233,30,99,0.04);
  transition: box-shadow 0.25s, transform 0.25s;
}
.stat-card:hover {
  box-shadow: 0 6px 20px rgba(233,30,99,0.1);
  transform: translateY(-2px);
}
.stat-card-inner { display: flex; align-items: center; justify-content: space-between; }
.stat-label { font-size: 13px; color: #888; margin-bottom: 6px; }
.stat-number { font-size: 28px; font-weight: 700; line-height: 1.2; color: #333; }
.stat-sub { font-size: 12px; color: #aaa; margin-top: 4px; }
.stat-icon {
  width: 46px; height: 46px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-icon.pink-bg { background: #fce4ec; color: #e91e63; }
.stat-icon.blue-bg { background: #e3f2fd; color: #42a5f5; }
.stat-icon.orange-bg { background: #fff3e0; color: #ff9800; }
.stat-icon.green-bg { background: #e8f5e9; color: #66bb6a; }

/* --- Panel Cards --- */
.panel-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 2px 12px rgba(233,30,99,0.04);
  overflow: hidden;
}
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 22px; border-bottom: 1px solid #fce4ec;
}
.panel-title { font-size: 15px; font-weight: 600; color: #333; }
.panel-sub { font-size: 12px; color: #aaa; margin-left: 8px; }
.panel-body { padding: 20px 22px; }

/* --- Recruitment Stages --- */
.stage-row { display: flex; align-items: center; gap: 10px; }
.stage-label { width: 70px; font-size: 13px; color: #666; flex-shrink: 0; }
.stage-track {
  flex: 1; height: 10px; background: #f5f5f5; border-radius: 5px; overflow: hidden;
}
.stage-bar { height: 100%; border-radius: 5px; transition: width 0.5s ease; }
.stage-count { width: 32px; text-align: right; font-size: 13px; font-weight: 600; color: #555; }
.progress-label { font-size: 12px; color: #888; margin-top: 8px; }

/* --- Ranking --- */
.ranking-title { font-size: 14px; font-weight: 600; color: #555; margin-bottom: 10px; }
.ranking-table { border-radius: 8px; overflow: hidden; }

/* --- Growth Chart --- */
.growth-chart {
  display: flex; align-items: flex-end; gap: 6px; height: 160px; padding-top: 10px;
}
.growth-col {
  flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%;
}
.growth-val { font-size: 10px; color: #aaa; margin-bottom: 4px; min-height: 14px; }
.growth-track {
  flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center;
}
.growth-bar {
  width: 70%; max-width: 28px; background: linear-gradient(180deg, #f48fb1, #e91e63);
  border-radius: 4px 4px 0 0; transition: height 0.5s ease; min-height: 2px;
}
.growth-label { font-size: 10px; color: #999; margin-top: 6px; }

/* --- Mini Stats --- */
.mini-stat { text-align: center; padding: 12px 0; }
.mini-stat-num { font-size: 24px; font-weight: 700; color: #333; }
.mini-stat-label { font-size: 12px; color: #888; margin-top: 4px; }

/* --- Element Plus overrides --- */
:deep(.el-progress-bar__outer) { border-radius: 8px; }
:deep(.el-progress-bar__inner) { border-radius: 8px; }
:deep(.el-select .el-input__wrapper) { border-radius: 8px !important; }
:deep(.el-select .el-input__wrapper:focus-within) { box-shadow: 0 0 0 1px #e91e63 !important; }
</style>
