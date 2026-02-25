<template>
  <div class="internship-management">
    <h2 class="page-title">实习期管理</h2>

    <!-- 筛选栏 -->
    <div class="card filter-bar">
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable @change="fetchList">
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="待转正" value="PENDING_CONVERSION" />
        <el-option label="待评估" value="PENDING_EVALUATION" />
        <el-option label="已转正" value="CONVERTED" />
        <el-option label="已延期" value="EXTENDED" />
        <el-option label="已终止" value="TERMINATED" />
      </el-select>
    </div>

    <!-- 实习列表 -->
    <div class="card">
      <el-table :data="internships" stripe @row-click="openDetail">
        <el-table-column prop="username" label="实习成员" width="120" />
        <el-table-column prop="mentorName" label="导师" width="120">
          <template #default="{ row }">{{ row.mentorName || '未指派' }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="expectedEndDate" label="结束日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成率" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.taskCompletionRate * 100)"
              :color="row.taskCompletionRate >= 0.8 ? '#10b981' : row.taskCompletionRate >= 0.5 ? '#e6a23c' : '#f56c6c'"
              :stroke-width="14"
              :text-inside="true"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING_CONVERSION'" type="success" size="small" @click.stop="handleConvert(row)">转正</el-button>
            <el-button v-if="row.status === 'IN_PROGRESS' || row.status === 'PENDING_EVALUATION'" type="warning" size="small" @click.stop="openExtendDialog(row)">延期</el-button>
            <el-button v-if="row.status === 'IN_PROGRESS' || row.status === 'PENDING_EVALUATION'" type="danger" size="small" @click.stop="handleTerminate(row)">终止</el-button>
            <el-button type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="'实习详情 - ' + (currentIntern?.username || '')" size="560px">
      <template v-if="currentIntern && currentProgress">
        <!-- 进度面板 -->
        <div class="progress-panel">
          <div class="progress-item">
            <span class="progress-label">任务完成率</span>
            <el-progress
              :percentage="Math.round(currentProgress.taskCompletionRate * 100)"
              :color="currentProgress.taskCompletionRate >= 0.8 ? '#10b981' : '#e6a23c'"
              :stroke-width="18"
              :text-inside="true"
            />
          </div>
          <div class="progress-stats">
            <div class="stat-item">
              <span class="stat-value">{{ currentProgress.totalPoints }}</span>
              <span class="stat-label">累计积分</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ currentProgress.remainingDays }}</span>
              <span class="stat-label">剩余天数</span>
            </div>
          </div>
        </div>

        <!-- 导师指派 -->
        <div class="section">
          <h4>导师指派</h4>
          <div class="mentor-row">
            <span>当前导师：{{ currentIntern.mentorName || '未指派' }}</span>
            <el-button type="primary" size="small" @click="mentorDialogVisible = true">指派导师</el-button>
          </div>
        </div>

        <!-- 任务清单 -->
        <div class="section">
          <div class="section-header">
            <h4>任务清单</h4>
            <el-button type="primary" size="small" @click="taskFormVisible = true">新建任务</el-button>
          </div>
          <el-table :data="currentProgress.tasks" stripe size="small">
            <el-table-column prop="taskName" label="任务名称" show-overflow-tooltip />
            <el-table-column prop="deadline" label="截止日期" width="110" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.completed ? 'success' : 'info'" size="small">
                  {{ row.completed ? '已完成' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button
                  v-if="!row.completed"
                  type="success"
                  size="small"
                  link
                  @click="handleCompleteTask(row)"
                >完成</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-drawer>

    <!-- 新建任务对话框 -->
    <el-dialog v-model="taskFormVisible" title="新建实习任务" width="460px">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务名称" required>
          <el-input v-model="taskForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.taskDescription" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="截止日期" required>
          <el-date-picker v-model="taskForm.deadline" type="date" value-format="YYYY-MM-DD" placeholder="选择截止日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSaving" @click="handleCreateTask">确定</el-button>
      </template>
    </el-dialog>

    <!-- 指派导师对话框 -->
    <el-dialog v-model="mentorDialogVisible" title="指派导师" width="400px">
      <el-form label-width="80px">
        <el-form-item label="导师">
          <el-select v-model="selectedMentorId" placeholder="选择导师" filterable>
            <el-option
              v-for="m in mentorCandidates"
              :key="m.id"
              :label="m.username"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mentorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignMentor">确定</el-button>
      </template>
    </el-dialog>

    <!-- 延期对话框 -->
    <el-dialog v-model="extendDialogVisible" title="延期实习" width="400px">
      <el-form label-width="100px">
        <el-form-item label="延期天数">
          <el-input-number v-model="extendDays" :min="1" :max="90" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExtend">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listInternships,
  getProgress,
  createTask,
  completeTask,
  assignMentor,
  approveConversion,
  extendInternship,
  terminateInternship,
} from '@/api/internship'
import type { InternshipListItemDTO, InternshipProgressDTO } from '@/api/internship'
import http from '@/api/axios'

// ---- List ----
const internships = ref<InternshipListItemDTO[]>([])
const statusFilter = ref<string>('')

async function fetchList() {
  try {
    const res = await listInternships(statusFilter.value || undefined)
    internships.value = res.data
  } catch { /* handled by interceptor */ }
}

// ---- Detail drawer ----
const drawerVisible = ref(false)
const currentIntern = ref<InternshipListItemDTO | null>(null)
const currentProgress = ref<InternshipProgressDTO | null>(null)

async function openDetail(row: InternshipListItemDTO) {
  currentIntern.value = row
  drawerVisible.value = true
  try {
    const res = await getProgress(row.id)
    currentProgress.value = res.data
  } catch { /* handled by interceptor */ }
}

// ---- Task management ----
const taskFormVisible = ref(false)
const taskSaving = ref(false)
const taskForm = reactive({ taskName: '', taskDescription: '', deadline: '' })

async function handleCreateTask() {
  if (!taskForm.taskName || !taskForm.deadline) {
    ElMessage.warning('请填写任务名称和截止日期')
    return
  }
  taskSaving.value = true
  try {
    await createTask(currentIntern.value!.id, {
      taskName: taskForm.taskName,
      taskDescription: taskForm.taskDescription,
      deadline: taskForm.deadline,
    })
    ElMessage.success('任务已创建')
    taskFormVisible.value = false
    taskForm.taskName = ''
    taskForm.taskDescription = ''
    taskForm.deadline = ''
    // Refresh progress
    const res = await getProgress(currentIntern.value!.id)
    currentProgress.value = res.data
    fetchList()
  } catch { /* handled */ } finally {
    taskSaving.value = false
  }
}

async function handleCompleteTask(task: { id: number }) {
  try {
    await completeTask(currentIntern.value!.id, task.id)
    ElMessage.success('任务已完成')
    const res = await getProgress(currentIntern.value!.id)
    currentProgress.value = res.data
    fetchList()
  } catch { /* handled */ }
}

// ---- Mentor assignment ----
const mentorDialogVisible = ref(false)
const selectedMentorId = ref<number | null>(null)
const mentorCandidates = ref<{ id: number; username: string }[]>([])

async function loadMentorCandidates() {
  try {
    const res = await http.get('/members', { params: { role: 'MEMBER,VICE_LEADER' } })
    mentorCandidates.value = (res as any).data || []
  } catch {
    mentorCandidates.value = []
  }
}

async function handleAssignMentor() {
  if (!selectedMentorId.value) {
    ElMessage.warning('请选择导师')
    return
  }
  try {
    await assignMentor(currentIntern.value!.id, selectedMentorId.value)
    ElMessage.success('导师已指派')
    mentorDialogVisible.value = false
    fetchList()
    // Refresh detail
    const res = await getProgress(currentIntern.value!.id)
    currentProgress.value = res.data
  } catch { /* handled */ }
}

// ---- Conversion ----
async function handleConvert(row: InternshipListItemDTO) {
  try {
    await ElMessageBox.confirm(`确认批准 ${row.username} 转正？`, '转正确认', { type: 'success' })
    await approveConversion(row.id)
    ElMessage.success('转正成功')
    fetchList()
    if (drawerVisible.value) drawerVisible.value = false
  } catch { /* cancelled or error */ }
}

// ---- Extend ----
const extendDialogVisible = ref(false)
const extendDays = ref(15)
let extendTarget: InternshipListItemDTO | null = null

function openExtendDialog(row: InternshipListItemDTO) {
  extendTarget = row
  extendDays.value = 15
  extendDialogVisible.value = true
}

async function handleExtend() {
  if (!extendTarget) return
  try {
    await extendInternship(extendTarget.id, extendDays.value)
    ElMessage.success('延期成功')
    extendDialogVisible.value = false
    fetchList()
  } catch { /* handled */ }
}

// ---- Terminate ----
async function handleTerminate(row: InternshipListItemDTO) {
  try {
    await ElMessageBox.confirm(`确认终止 ${row.username} 的实习？此操作不可撤销。`, '终止确认', { type: 'warning' })
    await terminateInternship(row.id)
    ElMessage.success('实习已终止')
    fetchList()
    if (drawerVisible.value) drawerVisible.value = false
  } catch { /* cancelled or error */ }
}

// ---- Status helpers ----
function statusTagType(status: string) {
  const map: Record<string, string> = {
    IN_PROGRESS: '',
    PENDING_CONVERSION: 'warning',
    PENDING_EVALUATION: 'warning',
    CONVERTED: 'success',
    EXTENDED: 'info',
    TERMINATED: 'danger',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    IN_PROGRESS: '进行中',
    PENDING_CONVERSION: '待转正',
    PENDING_EVALUATION: '待评估',
    CONVERTED: '已转正',
    EXTENDED: '已延期',
    TERMINATED: '已终止',
  }
  return map[status] || status
}

onMounted(() => {
  fetchList()
  loadMentorCandidates()
})
</script>

<style scoped>
.internship-management {
  max-width: 1100px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 20px;
}

.card {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  border: 1px solid rgba(16, 185, 129, 0.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  padding: 20px;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
}

.progress-panel {
  background: #f0fdf4;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.progress-item {
  margin-bottom: 12px;
}

.progress-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
  display: block;
}

.progress-stats {
  display: flex;
  gap: 32px;
  margin-top: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #10b981;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.section {
  margin-bottom: 20px;
}

.section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.section-header h4 {
  margin-bottom: 0;
}

.mentor-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #6b7280;
}
</style>
