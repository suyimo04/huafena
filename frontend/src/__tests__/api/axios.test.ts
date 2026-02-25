import { describe, it, expect, beforeEach } from 'vitest'
import instance from '@/api/axios'

describe('Axios Instance', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should have baseURL set to /api', () => {
    expect(instance.defaults.baseURL).toBe('/api')
  })

  it('should have timeout configured', () => {
    expect(instance.defaults.timeout).toBe(15000)
  })

  it('should have Content-Type header set to application/json', () => {
    expect(instance.defaults.headers['Content-Type']).toBe('application/json')
  })

  it('should have request and response interceptors registered', () => {
    // Axios stores interceptors internally; we verify they exist
    // @ts-ignore - accessing internal axios property for test
    const reqHandlers = (instance.interceptors.request as any).handlers
    // @ts-ignore
    const resHandlers = (instance.interceptors.response as any).handlers
    expect(reqHandlers.length).toBeGreaterThan(0)
    expect(resHandlers.length).toBeGreaterThan(0)
  })
})
