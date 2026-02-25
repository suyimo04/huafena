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
      <el-table-column label="封面" width="80" align="center">
        <template #default="{ row }">
          <el-image v-if="row.coverImageUrl" :src="row.coverImageUrl" fit="cover" class="w-12 h-12 rounded" />
          <span v-else class="text-gray-300 text-xs">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.activityType" size="small" type="info">{{ activityTypeLabel(row.activityType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地点" width="120" />
      <el-table-column prop="registrationCount" label="报名" width="70" align="center" />
      <el-table-column label="审核" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.approvalMode === 'MANUAL' ? 'warning' : 'success'" size="small">
            {{ row.approvalMode === 'MANUAL' ? '人工' : '自动' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="!canManage && row.status !== 'ARCHIVED'" size="small" type="primary" @click="handleRegister(row.id)">报名</el-button>
          <el-button v-if="!canManage && row.status !== 'ARCHIVED'" size="small" type="success" @click="handleCheckIn(row.id)">签到</el-button>
          <el-button v-if="canManage && row.status !== 'ARCHIVED'" size="small" type="warning" @click="handleArchive(row.id)">归档</el-button>
          <el-button v-if="canManage" size="small" @click="openQrCode(row)">二维码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create activity dialog -->
    <el-dialog v-model="createDialogVisible" title="创建活动" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动名称" required>
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入活动描述" />
        </el-form-item>
        <el-form-item label="封面图URL">
          <el-input v-model="form.coverImageUrl" placeholder="请输入封面图链接（可选）" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.activityType" placeholder="选择活动类型" clearable style="width: 100%">
            <el-option v-for="t in activityTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker v-model="form.eventTime" type="datetime" placeholder="选择活动时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="form.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="审核方式">
          <el-radio-group v-model="form.approvalMode">
            <el-radio value="AUTO">自动通过</el-radio>
            <el-radio value="MANUAL">人工审核</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报名表单">
          <el-input v-model="form.customFormFields" type="textarea" :rows="2" placeholder='自定义字段 JSON，如 [{"label":"手机号","type":"text"}]（可选）' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Activity detail drawer -->
    <el-drawer v-model="detailDrawerVisible" :title="'活动详情 - ' + (currentActivity?.name || '')" size="680px" destroy-on-close>
      <template v-if="currentActivity">
        <el-tabs v-model="detailTab">
          <!-- Statistics tab -->
          <el-tab-pane label="统计" name="statistics">
            <div v-if="statistics" class="grid grid-cols-2 gap-4 mb-4">
              <div class="bg-gray-50 rounded-xl p-4 text-center">
                <div class="text-2xl font-bold" style="color: #10b981">{{ statistics.totalRegistered }}</div>
                <div class="text-xs text-gray-500 mt-1">报名人数</div>
              </div>
              <div class="bg-gray-50 rounded-xl p-4 text-center">
                <div class="text-2xl font-bold" style="color: #10b981">{{ statistics.totalAttended }}</div>
                <div class="text-xs text-gray-500 mt-1">实际参与</div>
              </div>
              <div class="bg-gray-50 rounded-xl p-4 text-center">
                <div class="text-2xl font-bold" style="color: #10b981">{{ formatPercent(statistics.checkInRate) }}</div>
                <div class="text-xs text-gray-500 mt-1">签到率</div>
              </div>
              <div class="bg-gray-50 rounded-xl p-4 text-center">
                <div class="text-2xl font-bold" style="color: #10b981">{{ statistics.avgFeedbackRating?.toFixed(1) || '-' }}</div>
                <div class="text-xs text-gray-500 mt-1">平均评分</div>
              </div>
            </div>
            <el-button @click="loadStatistics" :loading="statsLoading" size="small">刷新统计</el-button>
          </el-tab-pane>

          <!-- Groups tab -->
          <el-tab-pane label="分组管理" name="groups">
            <div v-if="canManage" class="mb-4">
              <el-button type="primary" size="small" @click="showGroupForm = true">创建分组</el-button>
            </div>
            <div v-if="showGroupForm && canManage" class="mb-4 p-3 bg-gray-50 rounded-lg">
              <el-input v-model="groupForm.groupName" placeholder="分组名称" class="mb-2" />
              <el-input v-model="groupForm.memberIdsStr" placeholder="成员ID，逗号分隔（如 1,2,3）" class="mb-2" />
              <div class="flex gap-2">
                <el-button type="primary" size="small" @click="submitGroup">确定</el-button>
                <el-button size="small" @click="showGroupForm = false">取消</el-button>
              </div>
            </div>
            <el-table :data="groups" stripe size="small">
              <el-table-column prop="groupName" label="分组名称" />
              <el-table-column label="成员ID">
                <template #default="{ row }">{{ parseMemberIds(row.memberIds) }}</template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="160">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Feedback tab -->
          <el-tab-pane label="反馈" name="feedback">
            <div v-if="!canManage" class="mb-4 p-3 bg-gray-50 rounded-lg">
              <div class="mb-2 text-sm font-medium">提交反馈</div>
              <el-rate v-model="feedbackForm.rating" :colors="['#99A9BF', '#F7BA2A', '#10b981']" class="mb-2" />
              <el-input v-model="feedbackForm.comment" type="textarea" :rows="2" placeholder="请输入反馈意见" class="mb-2" />
              <el-button type="primary" size="small" @click="submitFeedbackAction">提交</el-button>
            </div>
            <el-table :data="feedbackList" stripe size="small">
              <el-table-column prop="userId" label="用户ID" width="80" />
              <el-table-column label="评分" width="80">
                <template #default="{ row }">{{ row.rating }} ⭐</template>
              </el-table-column>
              <el-table-column prop="comment" label="评论" />
              <el-table-column label="时间" width="160">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Materials tab -->
          <el-tab-pane label="资料" name="materials">
            <div v-if="canManage" class="mb-4">
              <el-upload
                :auto-upload="false"
                :on-change="handleFileChange"
                :show-file-list="false"
              >
                <el-button type="primary" size="small">选择文件</el-button>
              </el-upload>
              <div v-if="selectedFile" class="mt-2 flex items-center gap-2">
                <span class="text-sm text-gray-600">{{ selectedFile.name }}</span>
                <el-button type="success" size="small" @click="uploadMaterialAction" :loading="uploadingMaterial">上传</el-button>
              </div>
            </div>
            <el-table :data="materials" stripe size="small">
              <el-table-column prop="fileName" label="文件名" />
              <el-table-column prop="fileType" label="类型" width="100" />
              <el-table-column label="上传时间" width="160">
                <template #default="{ row }">{{ formatTime(row.uploadedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button size="small" link type="primary" @click="downloadMaterial(row)">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- QR Code tab -->
          <el-tab-pane v-if="canManage" label="签到二维码" name="qrcode">
            <div class="text-center py-4">
              <div v-if="qrToken" class="mb-4">
                <div class="inline-block p-6 bg-gray-50 rounded-xl border-2 border-dashed" style="border-color: #10b981">
                  <div class="text-lg font-mono font-bold" style="color: #10b981">{{ qrToken }}</div>
                </div>
                <div class="text-xs text-gray-400 mt-2">签到 Token（成员使用此 Token 签到）</div>
              </div>
              <el-button type="primary" @click="generateQrCodeAction" :loading="qrLoading">
                {{ qrToken ? '重新生成' : '生成签到二维码' }}
              </el-button>
            </div>
          </el-tab-pane>

          <!-- Registrations / Approval tab -->
          <el-tab-pane v-if="canManage && currentActivity.approvalMode === 'MANUAL'" label="报名审批" name="approval">
            <div class="text-sm text-gray-500 mb-2">人工审核模式 - 待审批的报名将在此显示</div>
            <p class="text-xs text-gray-400">请通过后端接口 POST /api/activities/{'{id}'}/registrations/{'{regId}'}/approve 审批报名</p>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- QR Code dialog -->
    <el-dialog v-model="qrDialogVisible" title="签到二维码" width="400px" destroy-on-close>
      <div class="text-center py-4">
        <div v-if="qrDialogToken" class="inline-block p-6 bg-gray-50 rounded-xl border-2 border-dashed" style="border-color: #10b981">
          <div class="text-xl font-mono font-bold" style="color: #10b981">{{ qrDialogToken }}</div>
        </div>
        <div v-else class="text-gray-400">正在生成...</div>
        <div class="text-xs text-gray-400 mt-3">成员使用此 Token 进行签到</div>
      </div>
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
  getGroups,
  createGroup,
  generateQrCode,
  submitFeedback,
  getFeedback,
  getStatistics,
  uploadMaterial,
  getMaterials,
} from '@/api/activity'
import type {
  ActivityDTO,
  ActivityGroupDTO,
  ActivityFeedbackDTO,
  ActivityMaterialDTO,
  ActivityStatisticsDTO,
  ActivityType,
  ApprovalMode,
} from '@/api/activity'

const authStore = useAuthStore()
const canManage = computed(() => ['ADMIN', 'LEADER'].includes(authStore.role))
const currentUserId = computed(() => authStore.user?.id)

/* ---- Activity list ---- */
const activities = ref<ActivityDTO[]>([])
const loading = ref(false)

const activityTypes = [
  { value: 'ONLINE', label: '线上活动' },
  { value: 'OFFLINE', label: '线下活动' },
  { value: 'TRAINING', label: '培训活动' },
  { value: 'TEAM_BUILDING', label: '团建活动' },
  { value: 'OTHER', label: '其他' },
]

const statusMap: Record<string, string> = {
  UPCOMING: '即将开始', ONGOING: '进行中', COMPLETED: '已完成', ARCHIVED: '已归档',
}
const statusTagTypeMap: Record<string, string> = {
  UPCOMING: 'info', ONGOING: 'success', COMPLETED: 'warning', ARCHIVED: '',
}

function statusLabel(s: string) { return statusMap[s] ?? s }
function statusTagType(s: string) { return statusTagTypeMap[s] ?? 'info' }
function activityTypeLabel(t: string) {
  return activityTypes.find(a => a.value === t)?.label ?? t
}
function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
function formatPercent(v: number | null | undefined) {
  if (v == null) return '-'
  return (Number(v) * 100).toFixed(1) + '%'
}
function parseMemberIds(json: string) {
  try { return JSON.parse(json).join(', ') } catch { return json }
}

async function fetchActivities() {
  loading.value = true
  try {
    const res = await listActivities()
    activities.value = res.data ?? []
  } catch { activities.value = [] }
  finally { loading.value = false }
}

/* ---- Create dialog ---- */
const createDialogVisible = ref(false)
const submitting = ref(false)
const form = ref({
  name: '', description: '', coverImageUrl: '', activityType: '' as ActivityType | '',
  eventTime: '' as any, location: '', approvalMode: 'AUTO' as ApprovalMode, customFormFields: '',
})

function openCreateDialog() {
  form.value = {
    name: '', description: '', coverImageUrl: '', activityType: '',
    eventTime: '', location: '', approvalMode: 'AUTO', customFormFields: '',
  }
  createDialogVisible.value = true
}

async function submitCreate() {
  if (!form.value.name) { ElMessage.warning('请输入活动名称'); return }
  if (!form.value.eventTime) { ElMessage.warning('请选择活动时间'); return }
  if (!form.value.location) { ElMessage.warning('请输入活动地点'); return }
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
      coverImageUrl: form.value.coverImageUrl || undefined,
      activityType: (form.value.activityType as ActivityType) || undefined,
      customFormFields: form.value.customFormFields || undefined,
      approvalMode: form.value.approvalMode,
    })
    ElMessage.success('活动创建成功')
    createDialogVisible.value = false
    await fetchActivities()
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

/* ---- Basic actions ---- */
async function handleRegister(activityId: number) {
  if (!currentUserId.value) return
  try {
    await registerForActivity(activityId, currentUserId.value)
    ElMessage.success('报名成功')
    await fetchActivities()
  } catch { /* interceptor */ }
}

async function handleCheckIn(activityId: number) {
  if (!currentUserId.value) return
  try {
    await checkInActivity(activityId, currentUserId.value)
    ElMessage.success('签到成功')
    await fetchActivities()
  } catch { /* interceptor */ }
}

async function handleArchive(activityId: number) {
  try {
    await archiveActivity(activityId)
    ElMessage.success('活动已归档')
    await fetchActivities()
  } catch { /* interceptor */ }
}

/* ---- Detail drawer ---- */
const detailDrawerVisible = ref(false)
const detailTab = ref('statistics')
const currentActivity = ref<ActivityDTO | null>(null)

// Statistics
const statistics = ref<ActivityStatisticsDTO | null>(null)
const statsLoading = ref(false)

// Groups
const groups = ref<ActivityGroupDTO[]>([])
const showGroupForm = ref(false)
const groupForm = ref({ groupName: '', memberIdsStr: '' })

// Feedback
const feedbackList = ref<ActivityFeedbackDTO[]>([])
const feedbackForm = ref({ rating: 5, comment: '' })

// Materials
const materials = ref<ActivityMaterialDTO[]>([])
const selectedFile = ref<File | null>(null)
const uploadingMaterial = ref(false)

// QR
const qrToken = ref('')
const qrLoading = ref(false)

async function openDetail(row: ActivityDTO) {
  currentActivity.value = row
  detailTab.value = 'statistics'
  detailDrawerVisible.value = true
  await Promise.all([loadStatistics(), loadGroups(), loadFeedback(), loadMaterials()])
  if (canManage.value) await loadQrToken()
}

async function loadStatistics() {
  if (!currentActivity.value) return
  statsLoading.value = true
  try {
    const res = await getStatistics(currentActivity.value.id)
    statistics.value = res.data
  } catch { statistics.value = null }
  finally { statsLoading.value = false }
}

async function loadGroups() {
  if (!currentActivity.value) return
  try {
    const res = await getGroups(currentActivity.value.id)
    groups.value = res.data ?? []
  } catch { groups.value = [] }
}

async function submitGroup() {
  if (!currentActivity.value || !groupForm.value.groupName) return
  const ids = groupForm.value.memberIdsStr.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
  try {
    await createGroup(currentActivity.value.id, groupForm.value.groupName, ids)
    ElMessage.success('分组创建成功')
    showGroupForm.value = false
    groupForm.value = { groupName: '', memberIdsStr: '' }
    await loadGroups()
  } catch { /* interceptor */ }
}

async function loadFeedback() {
  if (!currentActivity.value) return
  try {
    const res = await getFeedback(currentActivity.value.id)
    feedbackList.value = res.data ?? []
  } catch { feedbackList.value = [] }
}

async function submitFeedbackAction() {
  if (!currentActivity.value || !currentUserId.value) return
  if (!feedbackForm.value.rating) { ElMessage.warning('请选择评分'); return }
  try {
    await submitFeedback(currentActivity.value.id, currentUserId.value, feedbackForm.value.rating, feedbackForm.value.comment || undefined)
    ElMessage.success('反馈提交成功')
    feedbackForm.value = { rating: 5, comment: '' }
    await loadFeedback()
  } catch { /* interceptor */ }
}

async function loadMaterials() {
  if (!currentActivity.value) return
  try {
    const res = await getMaterials(currentActivity.value.id)
    materials.value = res.data ?? []
  } catch { materials.value = [] }
}

function handleFileChange(uploadFile: any) {
  selectedFile.value = uploadFile.raw
}

async function uploadMaterialAction() {
  if (!currentActivity.value || !selectedFile.value || !currentUserId.value) return
  uploadingMaterial.value = true
  try {
    await uploadMaterial(currentActivity.value.id, selectedFile.value, currentUserId.value)
    ElMessage.success('资料上传成功')
    selectedFile.value = null
    await loadMaterials()
  } catch { /* interceptor */ }
  finally { uploadingMaterial.value = false }
}

function downloadMaterial(row: ActivityMaterialDTO) {
  if (row.fileUrl) window.open(row.fileUrl, '_blank')
}

async function loadQrToken() {
  if (!currentActivity.value) return
  qrToken.value = currentActivity.value.qrToken || ''
}

async function generateQrCodeAction() {
  if (!currentActivity.value) return
  qrLoading.value = true
  try {
    const res = await generateQrCode(currentActivity.value.id)
    qrToken.value = res.data ?? ''
    ElMessage.success('签到二维码已生成')
  } catch { /* interceptor */ }
  finally { qrLoading.value = false }
}

/* ---- QR dialog (from table) ---- */
const qrDialogVisible = ref(false)
const qrDialogToken = ref('')

async function openQrCode(row: ActivityDTO) {
  qrDialogVisible.value = true
  qrDialogToken.value = ''
  try {
    const res = await generateQrCode(row.id)
    qrDialogToken.value = res.data ?? ''
  } catch { qrDialogToken.value = '生成失败' }
}

onMounted(() => { fetchActivities() })
</script>
