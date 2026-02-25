<template>
  <div class="login-page">
    <div class="login-card theme-card">
      <h2 class="login-title">花粉小组管理系统</h2>
      <p class="login-subtitle">请登录您的账户</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
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
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        还没有账户？
        <router-link to="/register" class="link-accent">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import api from '@/api/axios'
import type { ApiResponse } from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { getFirstAccessibleRoute } from '@/router/index'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = (await api.post('/auth/login', {
      username: form.username,
      password: form.password,
    })) as unknown as ApiResponse<{ token: string }>

    authStore.setToken(res.data.token)
    authStore.loadUserFromToken()

    ElMessage.success('登录成功')

    const redirect = (route.query.redirect as string) || getFirstAccessibleRoute(authStore.role)
    router.push(redirect)
  } catch {
    // Error already handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffffff, #f8fafc, #ecfdf5, #f0fdf4);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
}

.login-title {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 6px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 28px;
}

.login-btn {
  width: 100%;
  background-color: #10b981;
  border-color: #10b981;
}

.login-btn:hover {
  background-color: #059669;
  border-color: #059669;
}

.login-footer {
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
