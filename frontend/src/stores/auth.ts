import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  id: number
  username: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  const role = computed(() => user.value?.role ?? '')

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUser(userInfo: UserInfo) {
    user.value = userInfo
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  /** Decode basic user info from JWT payload (no verification). */
  function loadUserFromToken() {
    if (!token.value) return
    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      user.value = {
        id: payload.userId ?? payload.sub,
        username: payload.username ?? '',
        role: payload.role ?? '',
      }
    } catch {
      logout()
    }
  }

  // Attempt to restore user on store creation
  loadUserFromToken()

  return { token, user, isLoggedIn, role, setToken, setUser, logout, loadUserFromToken }
})
