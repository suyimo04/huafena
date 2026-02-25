import axios from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/** Unified backend response shape */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

/* ---- Request interceptor: attach JWT token ---- */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

/* ---- Response interceptor: unified response handling ---- */
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // Backend returns code 200 for success
    if (res.code === 200) {
      return res as any
    }
    // Business-level error
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          ElMessage.error(data?.message || '登录已过期，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error(data?.message || '无权访问该资源')
          break
        case 429:
          ElMessage.warning('请求过于频繁，请稍后再试')
          break
        default:
          ElMessage.error(data?.message || `请求错误 (${status})`)
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  },
)

export default instance
