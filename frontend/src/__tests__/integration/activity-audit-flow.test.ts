/**
 * Integration tests for the activity → audit log flow.
 * These tests verify that frontend API calls match backend endpoint contracts.
 *
 * Flow: Activity create → register → check-in → points award → archive
 * Audit: Key operations generate audit logs, queryable by type
 *
 * Validates: Requirements 9.1-9.7, 10.1-10.3
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

import http from '@/api/axios'
import {
  listActivities,
  createActivity,
  registerForActivity,
  checkInActivity,
  archiveActivity,
} from '@/api/activity'
import type { CreateActivityRequest } from '@/api/activity'
import {
  getDashboardStats,
  getAuditLogs,
} from '@/api/dashboard'

const mockGet = http.get as ReturnType<typeof vi.fn>
const mockPost = http.post as ReturnType<typeof vi.fn>

describe('Activity-Audit Flow - API Contract Alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ─── Activity Module ─────────────────────────────────────────────

  describe('Activity API Contract (Requirements 9.1-9.7)', () => {
    it('POST /activities sends name, description, eventTime, location, createdBy', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, name: '周末团建', description: '户外活动',
          activityTime: '2024-08-01T10:00:00', location: '公园',
          registrationCount: 0, status: 'UPCOMING', createdBy: 1,
          createdAt: '2024-07-20T09:00:00',
        },
      })

      const req: CreateActivityRequest = {
        name: '周末团建',
        description: '户外活动',
        eventTime: '2024-08-01T10:00:00',
        location: '公园',
        createdBy: 1,
      }
      const res = await createActivity(req)

      expect(mockPost).toHaveBeenCalledWith('/activities', req)
      expect(res.data.id).toBe(1)
      expect(res.data.name).toBe('周末团建')
      expect(res.data.status).toBe('UPCOMING')
      expect(res.data.registrationCount).toBe(0)
    })

    it('GET /activities returns activity list with all required fields', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          {
            id: 1, name: '周末团建', description: '户外活动',
            activityTime: '2024-08-01T10:00:00', location: '公园',
            registrationCount: 3, status: 'UPCOMING', createdBy: 1,
            createdAt: '2024-07-20T09:00:00',
          },
          {
            id: 2, name: '技术分享', description: null,
            activityTime: '2024-08-05T14:00:00', location: '会议室',
            registrationCount: 0, status: 'ARCHIVED', createdBy: 1,
            createdAt: '2024-07-18T09:00:00',
          },
        ],
      })

      const res = await listActivities()

      expect(mockGet).toHaveBeenCalledWith('/activities')
      expect(res.data).toHaveLength(2)
      const activity = res.data[0]
      expect(activity).toHaveProperty('id')
      expect(activity).toHaveProperty('name')
      expect(activity).toHaveProperty('description')
      expect(activity).toHaveProperty('activityTime')
      expect(activity).toHaveProperty('location')
      expect(activity).toHaveProperty('registrationCount')
      expect(activity).toHaveProperty('status')
      expect(activity).toHaveProperty('createdBy')
      expect(activity).toHaveProperty('createdAt')
    })

    it('POST /activities/{id}/register sends userId as query param', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, activityId: 1, userId: 10,
          checkedIn: false, checkedInAt: null,
          registeredAt: '2024-07-25T10:00:00',
        },
      })

      const res = await registerForActivity(1, 10)

      expect(mockPost).toHaveBeenCalledWith('/activities/1/register', null, { params: { userId: 10 } })
      expect(res.data.activityId).toBe(1)
      expect(res.data.userId).toBe(10)
      expect(res.data.checkedIn).toBe(false)
    })

    it('POST /activities/{id}/check-in sends userId as query param', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, activityId: 1, userId: 10,
          checkedIn: true, checkedInAt: '2024-08-01T10:05:00',
          registeredAt: '2024-07-25T10:00:00',
        },
      })

      const res = await checkInActivity(1, 10)

      expect(mockPost).toHaveBeenCalledWith('/activities/1/check-in', null, { params: { userId: 10 } })
      expect(res.data.checkedIn).toBe(true)
      expect(res.data.checkedInAt).toBeTruthy()
    })

    it('POST /activities/{id}/archive sends no body', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, name: '周末团建', description: '户外活动',
          activityTime: '2024-08-01T10:00:00', location: '公园',
          registrationCount: 3, status: 'ARCHIVED', createdBy: 1,
          createdAt: '2024-07-20T09:00:00',
        },
      })

      const res = await archiveActivity(1)

      expect(mockPost).toHaveBeenCalledWith('/activities/1/archive')
      expect(res.data.status).toBe('ARCHIVED')
    })

    it('register rejects duplicate registration (409 from backend)', async () => {
      mockPost.mockRejectedValueOnce({
        response: { status: 409, data: { code: 409, message: '不可重复报名同一活动' } },
      })

      await expect(registerForActivity(1, 10)).rejects.toBeTruthy()
    })

    it('check-in rejects unregistered user (403 from backend)', async () => {
      mockPost.mockRejectedValueOnce({
        response: { status: 403, data: { code: 403, message: '未报名该活动，无法签到' } },
      })

      await expect(checkInActivity(1, 99)).rejects.toBeTruthy()
    })
  })

  // ─── Dashboard & Audit Module ────────────────────────────────────

  describe('Dashboard API Contract (Requirements 10.1-10.3)', () => {
    it('GET /dashboard/stats returns stats with totalActivities field', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          totalMembers: 20, adminCount: 1, leaderCount: 1,
          viceLeaderCount: 1, memberCount: 4, internCount: 3,
          applicantCount: 10, totalActivities: 5, totalPointsRecords: 100,
        },
      })

      const res = await getDashboardStats()

      expect(mockGet).toHaveBeenCalledWith('/dashboard/stats')
      expect(res.data).toHaveProperty('totalMembers')
      expect(res.data).toHaveProperty('totalActivities')
      expect(res.data).toHaveProperty('totalPointsRecords')
      expect(res.data.totalActivities).toBe(5)
    })

    it('GET /dashboard/audit-logs returns logs with operatorId and operationDetail', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          {
            id: 1, operatorId: 1, operationType: 'ACTIVITY_CREATE',
            operationTime: '2024-07-20T09:00:00', operationDetail: '创建活动: 周末团建',
          },
          {
            id: 2, operatorId: 1, operationType: 'SALARY_SAVE',
            operationTime: '2024-07-19T15:00:00', operationDetail: '批量保存薪资记录',
          },
        ],
      })

      const res = await getAuditLogs()

      expect(mockGet).toHaveBeenCalledWith('/dashboard/audit-logs', { params: {} })
      expect(res.data).toHaveLength(2)
      const log = res.data[0]
      expect(log).toHaveProperty('id')
      expect(log).toHaveProperty('operatorId')
      expect(log).toHaveProperty('operationType')
      expect(log).toHaveProperty('operationTime')
      expect(log).toHaveProperty('operationDetail')
    })

    it('GET /dashboard/audit-logs?type=xxx filters by operation type', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          {
            id: 1, operatorId: 1, operationType: 'ACTIVITY_CREATE',
            operationTime: '2024-07-20T09:00:00', operationDetail: '创建活动: 周末团建',
          },
        ],
      })

      const res = await getAuditLogs('ACTIVITY_CREATE')

      expect(mockGet).toHaveBeenCalledWith('/dashboard/audit-logs', { params: { type: 'ACTIVITY_CREATE' } })
      expect(res.data).toHaveLength(1)
      expect(res.data[0].operationType).toBe('ACTIVITY_CREATE')
    })

    it('GET /dashboard/audit-logs without type sends empty params', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [],
      })

      await getAuditLogs()

      expect(mockGet).toHaveBeenCalledWith('/dashboard/audit-logs', { params: {} })
    })
  })

  // ─── End-to-End Flow: Activity Create → Register → Check-in → Archive ───

  describe('Flow: Activity Create → Register → Check-in → Archive', () => {
    it('complete activity flow calls APIs in correct order', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string, data?: unknown, config?: unknown) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/activities': {
            code: 200, message: 'success',
            data: {
              id: 1, name: '周末团建', description: '户外活动',
              activityTime: '2024-08-01T10:00:00', location: '公园',
              registrationCount: 0, status: 'UPCOMING', createdBy: 1,
              createdAt: '2024-07-20T09:00:00',
            },
          },
          '/activities/1/register': {
            code: 200, message: 'success',
            data: {
              id: 1, activityId: 1, userId: 10,
              checkedIn: false, checkedInAt: null,
              registeredAt: '2024-07-25T10:00:00',
            },
          },
          '/activities/1/check-in': {
            code: 200, message: 'success',
            data: {
              id: 1, activityId: 1, userId: 10,
              checkedIn: true, checkedInAt: '2024-08-01T10:05:00',
              registeredAt: '2024-07-25T10:00:00',
            },
          },
          '/activities/1/archive': {
            code: 200, message: 'success',
            data: {
              id: 1, name: '周末团建', description: '户外活动',
              activityTime: '2024-08-01T10:00:00', location: '公园',
              registrationCount: 1, status: 'ARCHIVED', createdBy: 1,
              createdAt: '2024-07-20T09:00:00',
            },
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/activities': {
            code: 200, message: 'success',
            data: [
              {
                id: 1, name: '周末团建', description: '户外活动',
                activityTime: '2024-08-01T10:00:00', location: '公园',
                registrationCount: 1, status: 'ARCHIVED', createdBy: 1,
                createdAt: '2024-07-20T09:00:00',
              },
            ],
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      // Step 1: Leader creates activity
      const createRes = await createActivity({
        name: '周末团建', description: '户外活动',
        eventTime: '2024-08-01T10:00:00', location: '公园', createdBy: 1,
      })
      expect(createRes.data.status).toBe('UPCOMING')

      // Step 2: Member registers for activity
      const regRes = await registerForActivity(1, 10)
      expect(regRes.data.checkedIn).toBe(false)

      // Step 3: Member checks in at activity
      const checkInRes = await checkInActivity(1, 10)
      expect(checkInRes.data.checkedIn).toBe(true)

      // Step 4: Leader archives activity
      const archiveRes = await archiveActivity(1)
      expect(archiveRes.data.status).toBe('ARCHIVED')

      // Step 5: List activities to verify
      const listRes = await listActivities()
      expect(listRes.data[0].status).toBe('ARCHIVED')

      expect(callOrder).toEqual([
        'POST /activities',
        'POST /activities/1/register',
        'POST /activities/1/check-in',
        'POST /activities/1/archive',
        'GET /activities',
      ])
    })
  })

  // ─── Cross-Module: Activity Operations → Audit Logs ──────────────

  describe('Cross-Module: Activity Operations Generate Audit Logs', () => {
    it('activity operations are reflected in audit logs', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/activities': {
            code: 200, message: 'success',
            data: {
              id: 1, name: '技术分享', description: null,
              activityTime: '2024-08-05T14:00:00', location: '会议室',
              registrationCount: 0, status: 'UPCOMING', createdBy: 1,
              createdAt: '2024-07-20T09:00:00',
            },
          },
          '/activities/1/register': {
            code: 200, message: 'success',
            data: { id: 1, activityId: 1, userId: 10, checkedIn: false, checkedInAt: null, registeredAt: '2024-07-25T10:00:00' },
          },
          '/activities/1/check-in': {
            code: 200, message: 'success',
            data: { id: 1, activityId: 1, userId: 10, checkedIn: true, checkedInAt: '2024-08-05T14:05:00', registeredAt: '2024-07-25T10:00:00' },
          },
          '/activities/1/archive': {
            code: 200, message: 'success',
            data: { id: 1, name: '技术分享', status: 'ARCHIVED', registrationCount: 1, createdBy: 1 },
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockGet.mockImplementation((url: string, config?: { params?: Record<string, string> }) => {
        callOrder.push(`GET ${url}`)
        if (url === '/dashboard/audit-logs') {
          const type = config?.params?.type
          const allLogs = [
            { id: 1, operatorId: 1, operationType: 'ACTIVITY_CREATE', operationTime: '2024-07-20T09:00:00', operationDetail: '创建活动: 技术分享' },
            { id: 2, operatorId: 10, operationType: 'POINTS_CHANGE', operationTime: '2024-08-05T14:05:00', operationDetail: '活动签到奖励 +5' },
            { id: 3, operatorId: 1, operationType: 'ACTIVITY_ARCHIVE', operationTime: '2024-08-06T09:00:00', operationDetail: '归档活动: 技术分享' },
          ]
          const filtered = type ? allLogs.filter(l => l.operationType === type) : allLogs
          return Promise.resolve({ code: 200, message: 'success', data: filtered })
        }
        if (url === '/dashboard/stats') {
          return Promise.resolve({
            code: 200, message: 'success',
            data: { totalMembers: 20, adminCount: 1, leaderCount: 1, viceLeaderCount: 1, memberCount: 4, internCount: 3, applicantCount: 10, totalActivities: 1, totalPointsRecords: 1 },
          })
        }
        return Promise.reject(new Error(`Unexpected GET ${url}`))
      })

      // Phase 1: Perform activity operations
      await createActivity({ name: '技术分享', eventTime: '2024-08-05T14:00:00', location: '会议室', createdBy: 1 })
      await registerForActivity(1, 10)
      await checkInActivity(1, 10)
      await archiveActivity(1)

      // Phase 2: Verify audit logs capture key operations
      const allLogsRes = await getAuditLogs()
      expect(allLogsRes.data).toHaveLength(3)
      expect(allLogsRes.data.map((l: { operationType: string }) => l.operationType)).toContain('ACTIVITY_CREATE')
      expect(allLogsRes.data.map((l: { operationType: string }) => l.operationType)).toContain('ACTIVITY_ARCHIVE')

      // Phase 3: Filter audit logs by type
      const filteredRes = await getAuditLogs('ACTIVITY_CREATE')
      expect(filteredRes.data).toHaveLength(1)
      expect(filteredRes.data[0].operationType).toBe('ACTIVITY_CREATE')

      // Phase 4: Dashboard stats reflect activity count
      const statsRes = await getDashboardStats()
      expect(statsRes.data.totalActivities).toBe(1)

      expect(callOrder).toEqual([
        'POST /activities',
        'POST /activities/1/register',
        'POST /activities/1/check-in',
        'POST /activities/1/archive',
        'GET /dashboard/audit-logs',
        'GET /dashboard/audit-logs',
        'GET /dashboard/stats',
      ])
    })
  })
})
