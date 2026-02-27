<template>
  <div class="applications-page p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-5">申请管理</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="申请列表" name="applications">
        <!-- Toolbar -->
        <div class="toolbar-card mb-5">
          <div class="flex items-center justify-between flex-wrap gap-3">
            <div class="flex items-center gap-2">
              <template v-if="selectedIds.length > 0">
                <span class="text-sm text-gray-500">已选 {{ selectedIds.length }} 项</span>
                <el-button class="btn-pink" size="small" @click="handleBatchApprove">
                  批量通过
                </el-button>
                <el-button class="btn-outline" size="small" @click="handleBatchReject">
                  批量拒绝
                </el-button>
                <el-button class="btn-outline" size="small" @click="handleBatchNotify">
                  批量发送面试通知
                </el-button>
              </template>
            </div>
            <div class="flex items-center gap-2">
              <el-button class="btn-outline" size="small" @click="handleExport">
                <el-icon class="mr-1"><Download /></el-icon>
                导出 Excel
              </el-button>
            </div>
          </div>
        </div>

        <div class="table-card">
        <el-table
          v-loading="loading"
          :data="applications"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="userId" label="申请者ID" width="100" />
          <el-table-column label="入口类型" width="120">
            <template #default="{ row }">
              {{ row.entryType === 'REGISTRATION' ? '注册' : '公开链接' }}
            </template>
          </el-table-column>
          <el-table-column label="中高考" width="90">
            <template #default="{ row }">
              <el-tag
                v-if="row.examFlag"
                type="danger"
                size="small"
                effect="dark"
                class="exam-tag"
              >
                {{ row.examType === 'GAOKAO' ? '高考' : row.examType === 'ZHONGKAO' ? '中考' : '备考' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="问卷摘要" min-width="180">
            <template #default="{ row }">
              <span v-if="row.questionnaireResponseId">
                问卷回答 #{{ row.questionnaireResponseId }}
              </span>
              <span v-else class="text-gray-400">无</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="160">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180" sortable prop="createdAt">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openDetail(row)">查看详情</el-button>
              <template v-if="row.status === 'PENDING_INITIAL_REVIEW'">
                <el-button size="small" type="success" @click="handleReview(row, true)">通过</el-button>
                <el-button size="small" type="danger" @click="handleReview(row, false)">拒绝</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        </div>

        <!-- Detail Dialog -->
        <el-dialog v-model="detailVisible" title="申请详情" width="700px">
          <template v-if="currentApp">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="申请ID">{{ currentApp.id }}</el-descriptions-item>
              <el-descriptions-item label="申请者ID">{{ currentApp.userId }}</el-descriptions-item>
              <el-descriptions-item label="入口类型">
                {{ currentApp.entryType === 'REGISTRATION' ? '注册' : '公开链接' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusTagType(currentApp.status)" size="small">
                  {{ statusLabel(currentApp.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApp.pollenUid" label="花粉UID">
                {{ currentApp.pollenUid }}
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApp.calculatedAge != null" label="年龄">
                {{ currentApp.calculatedAge }}
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApp.examFlag" label="中高考状态">
                <el-tag type="danger" size="small" effect="dark">
                  {{ currentApp.examType === 'GAOKAO' ? '高考' : '中考' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApp.needsAttention" label="重点审核">
                <el-tag type="warning" size="small">需关注</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间" :span="2">
                {{ formatTime(currentApp.createdAt) }}
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="questionnaireAnswers" class="mt-4">
              <h4 class="mb-2 font-medium">问卷回答</h4>
              <el-descriptions :column="1" border>
                <el-descriptions-item
                  v-for="(value, key) in questionnaireAnswers"
                  :key="key"
                  :label="String(key)"
                >
                  {{ typeof value === 'object' ? JSON.stringify(value) : String(value) }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- Timeline -->
            <div class="mt-4">
              <h4 class="mb-2 font-medium">流程时间线</h4>
              <ApplicationTimeline :application-id="currentApp.id" />
            </div>

            <div v-if="currentApp.status === 'PENDING_INITIAL_REVIEW'" class="mt-4 flex justify-end gap-2">
              <el-button type="success" @click="handleReview(currentApp, true)">通过</el-button>
              <el-button type="danger" @click="handleReview(currentApp, false)">拒绝</el-button>
            </div>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="公开链接" name="publicLinks">
        <PublicLinkManager />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import {
  getApplications,
  getQuestionnaireResponse,
  initialReview,
  batchApprove,
  batchReject,
  batchNotifyInterview,
  exportApplicationsExcel,
  type ApplicationDTO,
} from '@/api/application'
import PublicLinkManager from '@/components/application/PublicLinkManager.vue'
import ApplicationTimeline from '@/components/application/ApplicationTimeline.vue'

const activeTab = ref('applications')
const loading = ref(false)
const applications = ref<ApplicationDTO[]>([])
const detailVisible = ref(false)
const currentApp = ref<ApplicationDTO | null>(null)
const questionnaireAnswers = ref<Record<string, unknown> | null>(null)
const selectedRows = ref<ApplicationDTO[]>([])
const selectedIds = ref<number[]>([])

function handleSelectionChange(rows: ApplicationDTO[]) {
  selectedRows.value = rows
  selectedIds.value = rows.map((r) => r.id)
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING_INITIAL_REVIEW: 'warning',
    INITIAL_REVIEW_PASSED: 'success',
    REJECTED: 'danger',
    AUTO_REJECTED: 'danger',
    AI_INTERVIEW_IN_PROGRESS: '',
    PENDING_REVIEW: 'warning',
    INTERN_OFFERED: 'success',
    INTERNSHIP_IN_PROGRESS: '',
    MEMBER_CONVERTED: 'success',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_INITIAL_REVIEW: '待初审',
    INITIAL_REVIEW_PASSED: '初审通过',
    REJECTED: '已拒绝',
    AUTO_REJECTED: '自动筛选拒绝',
    AI_INTERVIEW_IN_PROGRESS: 'AI面试中',
    PENDING_REVIEW: '待复审',
    INTERN_OFFERED: '已发实习邀请',
    INTERNSHIP_IN_PROGRESS: '实习中',
    CONVERSION_REVIEW: '转正评审中',
    MEMBER_CONVERTED: '已转正',
  }
  return map[status] || status
}

function formatTime(dt: string) {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 19)
}

async function fetchApplications() {
  loading.value = true
  try {
    const res = await getApplications()
    applications.value = res.data
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function openDetail(app: ApplicationDTO) {
  currentApp.value = app
  questionnaireAnswers.value = null
  detailVisible.value = true

  if (app.questionnaireResponseId) {
    try {
      const res = await getQuestionnaireResponse(app.questionnaireResponseId)
      questionnaireAnswers.value = JSON.parse(res.data.answers)
    } catch {
      // silently fail – detail still shows basic info
    }
  }
}

async function handleReview(app: ApplicationDTO, approved: boolean) {
  const action = approved ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}该申请？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: approved ? 'success' : 'warning',
    })
  } catch {
    return // cancelled
  }

  try {
    await initialReview(app.id, { approved })
    ElMessage.success(`已${action}`)
    detailVisible.value = false
    await fetchApplications()
  } catch {
    // error handled by interceptor
  }
}

// Batch operations
async function handleBatchApprove() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定批量通过 ${selectedIds.value.length} 条申请？`,
      '批量通过',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'success' },
    )
  } catch {
    return
  }
  try {
    await batchApprove(selectedIds.value)
    ElMessage.success('批量通过成功')
    await fetchApplications()
  } catch {
    // handled by interceptor
  }
}

async function handleBatchReject() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定批量拒绝 ${selectedIds.value.length} 条申请？`,
      '批量拒绝',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await batchReject(selectedIds.value)
    ElMessage.success('批量拒绝成功')
    await fetchApplications()
  } catch {
    // handled by interceptor
  }
}

async function handleBatchNotify() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定向 ${selectedIds.value.length} 位申请者发送面试通知？`,
      '批量发送面试通知',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  try {
    await batchNotifyInterview(selectedIds.value)
    ElMessage.success('面试通知已发送')
    await fetchApplications()
  } catch {
    // handled by interceptor
  }
}

async function handleExport() {
  try {
    const blob = await exportApplicationsExcel()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `applications_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(fetchApplications)
</script>

<style scoped>
.applications-page {
  background: #fafafa;
  min-height: 100%;
}

/* --- Toolbar Card --- */
.toolbar-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 1px 6px rgba(233, 30, 99, 0.03);
}

/* --- Table Card --- */
.table-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 2px 12px rgba(233, 30, 99, 0.04);
  overflow: hidden;
}

/* --- Buttons --- */
.btn-pink {
  background: linear-gradient(135deg, #ec407a, #e91e63) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 8px !important;
  font-weight: 500 !important;
  transition: all 0.2s !important;
}
.btn-pink:hover:not(:disabled) {
  background: linear-gradient(135deg, #f06292, #ec407a) !important;
  box-shadow: 0 4px 14px rgba(233, 30, 99, 0.3) !important;
}
.btn-outline {
  background: #fff !important;
  color: #e91e63 !important;
  border: 1px solid #f8bbd0 !important;
  border-radius: 8px !important;
  font-weight: 500 !important;
  transition: all 0.2s !important;
}
.btn-outline:hover {
  background: #fce4ec !important;
  border-color: #e91e63 !important;
}

.exam-tag {
  font-weight: 600;
}
</style>
