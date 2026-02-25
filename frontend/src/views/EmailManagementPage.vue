<template>
  <div class="email-management">
    <h2 class="page-title">邮件管理</h2>

    <el-tabs v-model="activeTab" class="email-tabs">
      <!-- SMTP 配置 -->
      <el-tab-pane label="SMTP 配置" name="config">
        <div class="card">
          <el-form
            ref="configFormRef"
            :model="configForm"
            :rules="configRules"
            label-width="120px"
            class="config-form"
          >
            <el-form-item label="SMTP 主机" prop="smtpHost">
              <el-input v-model="configForm.smtpHost" placeholder="如 smtp.qq.com" />
            </el-form-item>
            <el-form-item label="SMTP 端口" prop="smtpPort">
              <el-input-number v-model="configForm.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="用户名" prop="smtpUsername">
              <el-input v-model="configForm.smtpUsername" placeholder="SMTP 登录用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="smtpPassword">
              <el-input
                v-model="configForm.smtpPassword"
                type="password"
                show-password
                placeholder="留空则不修改密码"
              />
            </el-form-item>
            <el-form-item label="发件人名称" prop="senderName">
              <el-input v-model="configForm.senderName" placeholder="如 花粉管理系统" />
            </el-form-item>
            <el-form-item label="启用 SSL">
              <el-switch v-model="configForm.sslEnabled" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="configSaving" @click="saveConfig">
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 邮件模板 -->
      <el-tab-pane label="邮件模板" name="templates">
        <div class="card">
          <el-table :data="templates" stripe>
            <el-table-column prop="templateCode" label="模板编码" width="240" />
            <el-table-column prop="subjectTemplate" label="邮件主题模板" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link @click="previewTemplate(row)">预览</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 模板预览对话框 -->
        <el-dialog v-model="previewVisible" title="模板预览" width="600px">
          <div class="preview-section">
            <p class="preview-label">主题：</p>
            <p class="preview-value">{{ previewData.subjectTemplate }}</p>
          </div>
          <div class="preview-section">
            <p class="preview-label">正文：</p>
            <div class="preview-body" v-html="previewData.bodyTemplate" />
          </div>
        </el-dialog>
      </el-tab-pane>

      <!-- 发送日志 -->
      <el-tab-pane label="发送日志" name="logs">
        <div class="card">
          <el-table :data="logs" stripe>
            <el-table-column prop="recipient" label="收件人" width="200" />
            <el-table-column prop="subject" label="主题" show-overflow-tooltip />
            <el-table-column prop="templateCode" label="模板" width="200">
              <template #default="{ row }">
                {{ row.templateCode || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="retryCount" label="重试" width="70" />
            <el-table-column prop="failReason" label="失败原因" show-overflow-tooltip />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">
                {{ row.sentAt || row.createdAt }}
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="logPage"
              :page-size="logPageSize"
              :total="logTotal"
              layout="total, prev, pager, next"
              @current-change="fetchLogs"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getEmailConfig,
  updateEmailConfig,
  getTemplates as fetchTemplatesApi,
  getLogs as fetchLogsApi,
} from '@/api/email'
import type { EmailTemplateDTO, EmailLogDTO } from '@/api/email'

const activeTab = ref('config')

// ---- SMTP 配置 ----
const configFormRef = ref<FormInstance>()
const configSaving = ref(false)
const configForm = reactive({
  smtpHost: '',
  smtpPort: 465,
  smtpUsername: '',
  smtpPassword: '',
  senderName: '',
  sslEnabled: true,
})

const configRules: FormRules = {
  smtpHost: [{ required: true, message: 'SMTP 主机不能为空', trigger: 'blur' }],
  smtpPort: [{ required: true, message: 'SMTP 端口不能为空', trigger: 'blur' }],
  smtpUsername: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
}

async function loadConfig() {
  try {
    const res = await getEmailConfig()
    const c = res.data
    configForm.smtpHost = c.smtpHost || ''
    configForm.smtpPort = c.smtpPort || 465
    configForm.smtpUsername = c.smtpUsername || ''
    configForm.smtpPassword = c.smtpPasswordEncrypted || ''
    configForm.senderName = c.senderName || ''
    configForm.sslEnabled = c.sslEnabled ?? true
  } catch {
    // Config may not exist yet, that's fine
  }
}

async function saveConfig() {
  const valid = await configFormRef.value?.validate().catch(() => false)
  if (!valid) return

  configSaving.value = true
  try {
    await updateEmailConfig({
      smtpHost: configForm.smtpHost,
      smtpPort: configForm.smtpPort,
      smtpUsername: configForm.smtpUsername,
      smtpPassword: configForm.smtpPassword || null,
      senderName: configForm.senderName,
      sslEnabled: configForm.sslEnabled,
    })
    ElMessage.success('SMTP 配置已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    configSaving.value = false
  }
}

// ---- 邮件模板 ----
const templates = ref<EmailTemplateDTO[]>([])
const previewVisible = ref(false)
const previewData = reactive({ subjectTemplate: '', bodyTemplate: '' })

async function fetchTemplates() {
  try {
    const res = await fetchTemplatesApi()
    templates.value = res.data
  } catch {
    // ignore
  }
}

function previewTemplate(row: EmailTemplateDTO) {
  previewData.subjectTemplate = row.subjectTemplate
  previewData.bodyTemplate = row.bodyTemplate
  previewVisible.value = true
}

// ---- 发送日志 ----
const logs = ref<EmailLogDTO[]>([])
const logPage = ref(1)
const logPageSize = 20
const logTotal = ref(0)

async function fetchLogs() {
  try {
    const res = await fetchLogsApi(logPage.value - 1, logPageSize)
    logs.value = res.data.content
    logTotal.value = res.data.totalElements
  } catch {
    // ignore
  }
}

function statusTagType(status: string) {
  switch (status) {
    case 'SENT': return 'success'
    case 'FAILED': return 'danger'
    case 'PENDING': return 'warning'
    default: return 'info'
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'SENT': return '已发送'
    case 'FAILED': return '失败'
    case 'PENDING': return '待发送'
    default: return status
  }
}

onMounted(() => {
  loadConfig()
  fetchTemplates()
  fetchLogs()
})
</script>

<style scoped>
.email-management {
  max-width: 960px;
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
  padding: 24px;
}

.config-form {
  max-width: 520px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.preview-section {
  margin-bottom: 16px;
}

.preview-label {
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.preview-value {
  color: #6b7280;
}

.preview-body {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}
</style>
