import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should start logged out when no token in localStorage', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
  })

  it('should set token and persist to localStorage', () => {
    const store = useAuthStore()
    store.setToken('test-jwt-token')
    expect(store.token).toBe('test-jwt-token')
    expect(localStorage.getItem('token')).toBe('test-jwt-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('should set user info', () => {
    const store = useAuthStore()
    store.setUser({ id: 1, username: 'admin', role: 'ADMIN' })
    expect(store.user?.username).toBe('admin')
    expect(store.role).toBe('ADMIN')
  })

  it('should clear state on logout', () => {
    const store = useAuthStore()
    store.setToken('some-token')
    store.setUser({ id: 1, username: 'admin', role: 'ADMIN' })
    store.logout()
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('should decode user from a valid JWT token in localStorage', () => {
    // Create a fake JWT with base64-encoded payload
    const payload = { userId: 42, username: 'leader', role: 'LEADER' }
    const fakeJwt = `header.${btoa(JSON.stringify(payload))}.signature`
    localStorage.setItem('token', fakeJwt)

    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(true)
    expect(store.user?.id).toBe(42)
    expect(store.user?.username).toBe('leader')
    expect(store.role).toBe('LEADER')
  })

  it('should logout if token in localStorage is malformed', () => {
    localStorage.setItem('token', 'not-a-jwt')
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.user).toBeNull()
  })
})
