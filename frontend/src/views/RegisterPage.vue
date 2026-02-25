<template>
  <div class="register-page">
    <div class="register-card theme-card">
      <h2 class="register-title">注册新账户</h2>
      <p class="register-subtitle">填写信息以申请加入花粉小组</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            :disabled="loading"
          />
        </el-form-item>

        <el-divider>申请信息</el-divider>

        <!-- 花粉 UID (QQ号) -->
        <el-form-item label="花粉 UID（QQ号）" prop="pollenUid">
          <el-input
            v-model="form.pollenUid"
            placeholder="请输入QQ号（5-11位纯数字）"
            size="large"
            :disabled="loading"
          />
        </el-form-item>

        <!-- 出生年月 -->
        <el-form-item label="出生年月" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            placeholder="请选择出生日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            size="large"
            :disabled="loading"
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
            v-model="form.educationStage"
            placeholder="请选择教育阶段"
            size="large"
            :disabled="loading"
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
          <el-checkbox v-model="form.examFlag" :disabled="loading">
            即将参加中考或高考
          </el-checkbox>
        </el-form-item>

        <template v-if="form.examFlag">
          <el-form-item label="考试类型" prop="examType">
            <el-select
              v-model="form.examType"
              placeholder="请选择考试类型"
              size="large"
              :disabled="loading"
              style="width: 100%"
            >
              <el-option label="中考" value="ZHONGKAO" />
              <el-option label="高考" value="GAOKAO" />
            </el-select>
          </el-form-item>

          <el-form-item label="考试日期" prop="examDate">
            <el-date-picker
              v-model="form.examDate"
              type="date"
              placeholder="请选择考试日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              size="large"
              :disabled="loading"
              style="width: 100%"
            />
          </el-form-item>
        </template>

        <el-divider>可用性承诺</el-divider>

        <!-- 可用性承诺 -->
        <el-form-item label="每周可用天数" prop="weeklyAvailableDays">
          <el-input-number
            v-model="form.weeklyAvailableDays"
            :min="1"
            :max="7"
            size="large"
            :disabled="loading"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="每日可用时长（小时）" prop="dailyAvailableHours">
          <el-input-number
            v-model="form.dailyAvailableHours"
            :min="0.5"
            :max="24"
            :step="0.5"
            :precision="1"
            size="large"
            :disabled="loading"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="每周可用时段" prop="weeklyAvailableSlots">
          <el-select
            v-model="form.weeklyAvailableSlots"
            multiple
            placeholder="请选择可用时段"
            size="large"
            :disabled="loading"
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

        <!-- Questionnaire placeholder -->
        <div class="questionnaire-placeholder">
          <el-divider>申请问卷</el-divider>
          <div class="questionnaire-area">
            <el-icon size="32" color="#9ca3af"><Document /></el-icon>
            <p>问卷将在此处加载</p>
          </div>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        已有账户？
        <router-link to="/login" class="link-accent">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import api from '@/api/axios'
import { calculateAge } from '@/utils/formHelpers'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
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
  if (!form.birthDate) return null
  return calculateAge(form.birthDate)
})

function disableFutureDate(date: Date) {
  return date.getTime() > Date.now()
}

// Clear exam fields when examFlag is unchecked
watch(() => form.examFlag, (val) => {
  if (!val) {
    form.examType = ''
    form.examDate = ''
  }
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

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
  if (form.examFlag && !value) {
    callback(new Error('请选择考试类型'))
  } else {
    callback()
  }
}

const validateExamDate = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (form.examFlag && !value) {
    callback(new Error('请选择考试日期'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为 6-50 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
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

async function handleRegister() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await api.post('/auth/register', {
      username: form.username,
      password: form.password,
      pollenUid: form.pollenUid,
      birthDate: form.birthDate,
      educationStage: form.educationStage,
      examFlag: form.examFlag,
      examType: form.examFlag ? form.examType : null,
      examDate: form.examFlag ? form.examDate : null,
      weeklyAvailableDays: form.weeklyAvailableDays,
      dailyAvailableHours: form.dailyAvailableHours,
      weeklyAvailableSlots: form.weeklyAvailableSlots,
    })

    ElMessage.success('注册成功，请等待审核后登录')
    router.push('/login')
  } catch {
    // Error already handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffffff, #f8fafc, #ecfdf5, #f0fdf4);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 560px;
  padding: 40px 36px;
}

.register-title {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 6px;
}

.register-subtitle {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 28px;
}

.age-display {
  margin: -12px 0 16px;
}

.questionnaire-placeholder {
  margin-bottom: 20px;
}

.questionnaire-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  border: 2px dashed #e5e7eb;
  border-radius: 12px;
  color: #9ca3af;
  font-size: 14px;
}

.questionnaire-area p {
  margin: 8px 0 0;
}

.register-btn {
  width: 100%;
  background-color: #10b981;
  border-color: #10b981;
}

.register-btn:hover {
  background-color: #059669;
  border-color: #059669;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin-top: 16px;
}

.link-accent {
  color: #10b981;
  text-decoration: none;
  font-weight: 500;
}

.link-accent:hover {
  color: #059669;
  text-decoration: underline;
}
</style>
