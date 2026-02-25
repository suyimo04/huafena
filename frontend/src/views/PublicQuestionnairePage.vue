<template>
  <div class="public-questionnaire-page">
    <div class="public-card theme-card">
      <!-- Loading -->
      <div v-if="loading" class="state-container">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p class="state-text">正在加载问卷…</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="state-container">
        <el-icon :size="48" color="#f56c6c"><CircleCloseFilled /></el-icon>
        <p class="state-text error-text">{{ error }}</p>
        <el-button type="primary" @click="fetchQuestionnaire">重试</el-button>
      </div>

      <!-- Success after submit -->
      <div v-else-if="submitted" class="state-container success-container">
        <el-icon :size="48" color="#10b981"><CircleCheckFilled /></el-icon>
        <h2 class="success-title">提交成功</h2>
        <p class="state-text">您的问卷已成功提交，以下是您的账户信息，请妥善保管：</p>
        <div class="account-info">
          <div class="info-row">
            <span class="info-label">用户名：</span>
            <span class="info-value">{{ accountInfo.username }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">密码：</span>
            <span class="info-value">{{ accountInfo.password }}</span>
          </div>
        </div>
        <el-button type="primary" @click="goToLogin">前往登录</el-button>
      </div>

      <!-- Questionnaire form -->
      <template v-else-if="schema">
        <h2 class="form-title">问卷填写</h2>

        <el-form
          ref="appFormRef"
          :model="appForm"
          :rules="appRules"
          label-position="top"
        >
          <el-divider>申请信息</el-divider>

          <!-- 花粉 UID (QQ号) -->
          <el-form-item label="花粉 UID（QQ号）" prop="pollenUid">
            <el-input
              v-model="appForm.pollenUid"
              placeholder="请输入QQ号（5-11位纯数字）"
              size="large"
              :disabled="submitting"
            />
          </el-form-item>

          <!-- 出生年月 -->
          <el-form-item label="出生年月" prop="birthDate">
            <el-date-picker
              v-model="appForm.birthDate"
              type="date"
              placeholder="请选择出生日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              size="large"
              :disabled="submitting"
              :disabled-date="disableFutureDate"
              style="width: 100%"
            />
          </el-form-item>
          <div v-if="calculatedAge !== null" class="age-display">
            <el-tag :type="calculatedAge < 18 ? 'danger' : 'success'" size="small">
              当前年龄：{{ calculatedAge }} 岁
            </el-tag>
          </div>

          <!-- 学生身份 -->
          <el-form-item label="教育阶段" prop="educationStage">
            <el-select
              v-model="appForm.educationStage"
              placeholder="请选择教育阶段"
              size="large"
              :disabled="submitting"
              style="width: 100%"
            >
              <el-option label="初中" value="MIDDLE_SCHOOL" />
              <el-option label="高中" value="HIGH_SCHOOL" />
              <el-option label="大学" value="UNIVERSITY" />
              <el-option label="研究生" value="GRADUATE" />
              <el-option label="非学生" value="NON_STUDENT" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="appForm.examFlag" :disabled="submitting">
              即将参加中考或高考
            </el-checkbox>
          </el-form-item>

          <template v-if="appForm.examFlag">
            <el-form-item label="考试类型" prop="examType">
              <el-select
                v-model="appForm.examType"
                placeholder="请选择考试类型"
                size="large"
                :disabled="submitting"
                style="width: 100%"
              >
                <el-option label="中考" value="ZHONGKAO" />
                <el-option label="高考" value="GAOKAO" />
              </el-select>
            </el-form-item>

            <el-form-item label="考试日期" prop="examDate">
              <el-date-picker
                v-model="appForm.examDate"
                type="date"
                placeholder="请选择考试日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                size="large"
                :disabled="submitting"
                style="width: 100%"
              />
            </el-form-item>
          </template>

          <el-divider>可用性承诺</el-divider>

          <el-form-item label="每周可用天数" prop="weeklyAvailableDays">
            <el-input-number
              v-model="appForm.weeklyAvailableDays"
              :min="1"
              :max="7"
              size="large"
              :disabled="submitting"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="每日可用时长（小时）" prop="dailyAvailableHours">
            <el-input-number
              v-model="appForm.dailyAvailableHours"
              :min="0.5"
              :max="24"
              :step="0.5"
              :precision="1"
              size="large"
              :disabled="submitting"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="每周可用时段" prop="weeklyAvailableSlots">
            <el-select
              v-model="appForm.weeklyAvailableSlots"
              multiple
              placeholder="请选择可用时段"
              size="large"
              :disabled="submitting"
              style="width: 100%"
            >
              <el-option label="工作日上午 (9:00-12:00)" value="WEEKDAY_MORNING" />
              <el-option label="工作日下午 (13:00-18:00)" value="WEEKDAY_AFTERNOON" />
              <el-option label="工作日晚上 (19:00-22:00)" value="WEEKDAY_EVENING" />
              <el-option label="周末上午 (9:00-12:00)" value="WEEKEND_MORNING" />
              <el-option label="周末下午 (13:00-18:00)" value="WEEKEND_AFTERNOON" />
              <el-option label="周末晚上 (19:00-22:00)" value="WEEKEND_EVENING" />
            </el-select>
          </el-form-item>
        </el-form>

        <el-divider>问卷内容</el-divider>
        <QuestionnaireRenderer
          ref="rendererRef"
          :schema="schema"
          @submit="handleSubmit"
        />
        <div v-if="submitting" class="submitting-overlay">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span>提交中…</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { QuestionnaireSchema } from '@/types/questionnaire'
import { getPublicQuestionnaire, submitPublicQuestionnaire } from '@/api/questionnaire'
import QuestionnaireRenderer from '@/components/questionnaire/QuestionnaireRenderer.vue'
import { calculateAge } from '@/utils/formHelpers'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const schema = ref<QuestionnaireSchema | null>(null)
const submitting = ref(false)
const submitted = ref(false)
const accountInfo = ref({ username: '', password: '' })
const rendererRef = ref<InstanceType<typeof QuestionnaireRenderer> | null>(null)
const appFormRef = ref<FormInstance>()

const linkToken = route.params.linkToken as string

const appForm = reactive({
  pollenUid: '',
  birthDate: '',
  educationStage: '',
  examFlag: false,
  examType: '',
  examDate: '',
  weeklyAvailableDays: 3,
  dailyAvailableHours: 2,
  weeklyAvailableSlots: [] as string[],
})

const calculatedAge = computed(() => {
  if (!appForm.birthDate) return null
  return calculateAge(appForm.birthDate)
})

function disableFutureDate(date: Date) {
  return date.getTime() > Date.now()
}

// Clear exam fields when examFlag is unchecked
watch(() => appForm.examFlag, (val) => {
  if (!val) {
    appForm.examType = ''
    appForm.examDate = ''
  }
})

const validatePollenUid = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入花粉 UID（QQ号）'))
    return
  }
  if (!/^[1-9]\d{4,10}$/.test(value)) {
    callback(new Error('QQ号格式不正确，需为5-11位纯数字且首位不为0'))
    return
  }
  callback()
}

const validateExamType = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (appForm.examFlag && !value) {
    callback(new Error('请选择考试类型'))
  } else {
    callback()
  }
}

const validateExamDate = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (appForm.examFlag && !value) {
    callback(new Error('请选择考试日期'))
  } else {
    callback()
  }
}

const appRules: FormRules = {
  pollenUid: [
    { required: true, validator: validatePollenUid, trigger: 'blur' },
  ],
  birthDate: [
    { required: true, message: '请选择出生日期', trigger: 'change' },
  ],
  educationStage: [
    { required: true, message: '请选择教育阶段', trigger: 'change' },
  ],
  examType: [
    { validator: validateExamType, trigger: 'change' },
  ],
  examDate: [
    { validator: validateExamDate, trigger: 'change' },
  ],
  weeklyAvailableDays: [
    { required: true, message: '请填写每周可用天数', trigger: 'change' },
  ],
  dailyAvailableHours: [
    { required: true, message: '请填写每日可用时长', trigger: 'change' },
  ],
  weeklyAvailableSlots: [
    { required: true, message: '请选择至少一个可用时段', trigger: 'change', type: 'array', min: 1 },
  ],
}

async function fetchQuestionnaire() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPublicQuestionnaire(linkToken)
    schema.value = res.data
  } catch {
    error.value = '链接无效或已过期'
  } finally {
    loading.value = false
  }
}

async function handleSubmit(answers: Record<string, unknown>) {
  // Validate application form first
  if (appFormRef.value) {
    const valid = await appFormRef.value.validate().catch(() => false)
    if (!valid) return
  }

  submitting.value = true
  try {
    const res = await submitPublicQuestionnaire(linkToken, {
      ...answers,
      pollenUid: appForm.pollenUid,
      birthDate: appForm.birthDate,
      educationStage: appForm.educationStage,
      examFlag: appForm.examFlag,
      examType: appForm.examFlag ? appForm.examType : null,
      examDate: appForm.examFlag ? appForm.examDate : null,
      weeklyAvailableDays: appForm.weeklyAvailableDays,
      dailyAvailableHours: appForm.dailyAvailableHours,
      weeklyAvailableSlots: appForm.weeklyAvailableSlots,
    })
    accountInfo.value = res.data
    submitted.value = true
  } catch {
    // Error already shown by axios interceptor
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push('/login')
}

onMounted(fetchQuestionnaire)
</script>

<style scoped>
.public-questionnaire-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #ffffff, #f8fafc, #ecfdf5, #f0fdf4);
}

.public-card {
  width: 100%;
  max-width: 680px;
  padding: 32px;
  position: relative;
}

.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 0;
}

.state-text {
  color: #606266;
  font-size: 14px;
}

.error-text {
  color: #f56c6c;
}

.success-container {
  text-align: center;
}

.success-title {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.account-info {
  background: #f0fdf4;
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  padding: 16px 24px;
  width: 100%;
  max-width: 320px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.info-label {
  color: #909399;
}

.info-value {
  font-weight: 600;
  color: #303133;
}

.form-title {
  font-size: 20px;
  color: #303133;
  margin: 0 0 24px;
  text-align: center;
}

.age-display {
  margin: -12px 0 16px;
}

.submitting-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: var(--card-radius);
  z-index: 10;
}
</style>
