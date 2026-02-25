<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">报表与数据导出</h2>

    <!-- Export Buttons Card -->
    <el-card class="report-card mb-6">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-semibold text-gray-700">数据导出</span>
        </div>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Quick Export -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">快速导出</h4>
          <div class="flex flex-wrap gap-3">
            <el-button type="primary" @click="handleExport('members')" :loading="exporting === 'members'">
              导出成员
            </el-button>
            <el-button type="primary" @click="handleExport('points')" :loading="exporting === 'points'">
              导出积分
            </el-button>
            <el-button type="primary" @click="handleExport('salary')" :loading="exporting === 'salary'">
              导出薪资
            </el-button>
            <el-button type="primary" @click="handleExport('activities')" :loading="exporting === 'activities'">
              导出活动
            </el-button>
          </div>
        </div>
        <!-- Custom Date Range Export -->
        <div>
          <h4 class="text-sm font-medium text-gray-600 mb-3">自定义时间段导出</h4>
          <div class="flex flex-wrap items-end gap-3">
            <el-select v-model="customDataType" placeholder="数据类型" style="width: 130px">
              <el-option label="成员" value="members" />
              <el-option label="积分" value="points" />
              <el-option label="薪资" value="salary" />
              <el-option label="活动" value="activities" />
            </el-select>
            <el-date-picker
              v-model="customDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
            <el-button
              type="success"
              @click="handleCustomExport"
              :loading="exporting === 'custom'"
              :disabled="!customDataType || !customDateRange"
            >
              导出
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Weekly Reports Section -->
    <el-card class="report-card mb-6">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-semibold text-gray-700">运营周报</span>
          <el-button type="primary" size="small" @click="handleGenerate" :loading="generating">
            手动生成周报
          </el-button>
        </div>
      </template>

      <el-table :data="reports" v-loading="loading" stripe class="w-full">
        <el-table-column label="周报时间范围" min-width="180">
          <template #default="{ row }">{{ row.weekStart }} ~ {{ row.weekEnd }}</template>
        </el-table-column>
        <el-table-column prop="newApplications" label="新增申请" width="100" align="center" />
        <el-table-column prop="interviewsCompleted" label="面试完成" width="100" align="center" />
        <el-table-column prop="newMembers" label="新增成员" width="100" align="center" />
        <el-table-column prop="activitiesHeld" label="活动举办" width="100" align="center" />
        <el-table-column prop="totalPointsIssued" label="积分发放" width="100" align="center" />
        <el-table-column label="生成时间" width="180">
          <template #default="{ row }">{{ formatTime(row.generatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Weekly Report Detail Dialog -->
    <el-dialog v-model="detailVisible" title="周报详情" width="560px">
      <template v-if="selectedReport">
        <div class="mb-4 text-center">
          <span class="text-lg font-semibold text-gray-700">
            {{ selectedReport.weekStart }} ~ {{ selectedReport.weekEnd }}
          </span>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="detail-stat">
            <div class="detail-label">新增申请数</div>
            <div class="detail-value">{{ selectedReport.newApplications }}</div>
          </div>
          <div class="detail-stat">
            <div class="detail-label">面试完成数</div>
            <div class="detail-value">{{ selectedReport.interviewsCompleted }}</div>
          </div>
          <div class="detail-stat">
            <div class="detail-label">新增成员数</div>
            <div class="detail-value">{{ selectedReport.newMembers }}</div>
          </div>
          <div class="detail-stat">
            <div class="detail-label">活动举办数</div>
            <div class="detail-value">{{ selectedReport.activitiesHeld }}</div>
          </div>
          <div class="detail-stat col-span-2">
            <div class="detail-label">积分发放总量</div>
            <div class="detail-value">{{ selectedReport.totalPointsIssued }}</div>
          </div>
        </div>
        <div class="mt-4 text-xs text-gray-400 text-right">
          生成时间：{{ formatTime(selectedReport.generatedAt) }}
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listWeeklyReports,
  generateWeeklyReport,
  exportMembers,
  exportPoints,
  exportSalary,
  exportActivities,
  exportCustom,
} from '@/api/report'
import type { WeeklyReportDTO } from '@/api/report'

const reports = ref<WeeklyReportDTO[]>([])
const loading = ref(false)
const generating = ref(false)
const exporting = ref<string | null>(null)

const customDataType = ref('')
const customDateRange = ref<[string, string] | null>(null)

const detailVisible = ref(false)
const selectedReport = ref<WeeklyReportDTO | null>(null)

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function fetchReports() {
  loading.value = true
  try {
    const res = await listWeeklyReports()
    reports.value = res.data ?? []
  } catch {
    reports.value = []
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    await generateWeeklyReport()
    ElMessage.success('周报生成成功')
    await fetchReports()
  } catch {
    // handled by interceptor
  } finally {
    generating.value = false
  }
}

function showDetail(report: WeeklyReportDTO) {
  selectedReport.value = report
  detailVisible.value = true
}

const exportFns: Record<string, () => Promise<void>> = {
  members: exportMembers,
  points: exportPoints,
  salary: exportSalary,
  activities: exportActivities,
}

async function handleExport(type: string) {
  exporting.value = type
  try {
    await exportFns[type]()
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = null
  }
}

async function handleCustomExport() {
  if (!customDataType.value || !customDateRange.value) return
  exporting.value = 'custom'
  try {
    await exportCustom(customDataType.value, customDateRange.value[0], customDateRange.value[1])
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = null
  }
}

onMounted(() => {
  fetchReports()
})
</script>

<style scoped>
.report-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.detail-stat {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
}
.detail-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
.detail-value {
  font-size: 28px;
  font-weight: 600;
  color: #10b981;
}
</style>
