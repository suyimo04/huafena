/**
 * Integration tests for the points → salary → member rotation flow.
 * These tests verify that frontend API calls match backend endpoint contracts.
 *
 * Flow 1: Points records → checkin calculation → mini coin conversion → salary calculation → batch save → archive
 * Flow 2: Points monitoring → promotion review trigger → role rotation
 *
 * Validates: Requirements 6.1-6.5, 7.1-7.12, 8.1-8.4
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
  getPointsRecords,
  getTotalPoints,
  addPoints,
  deductPoints,
} from '@/api/points'
import type { AddPointsRequest, DeductPointsRequest } from '@/api/points'
import {
  getSalaryList,
  batchSaveSalary,
  calculateSalaries,
  archiveSalary,
} from '@/api/salary'
import type { SalaryRecord, BatchSaveRequest } from '@/api/salary'
import {
  checkPromotionEligibility,
  checkDemotionCandidates,
  triggerPromotionReview,
  executePromotion,
  markForDismissal,
  getPendingDismissalList,
} from '@/api/members'

const mockGet = http.get as ReturnType<typeof vi.fn>
const mockPost = http.post as ReturnType<typeof vi.fn>
const mockPut = http.put as ReturnType<typeof vi.fn>

describe('Points-Salary-Rotation Flow - API Contract Alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ─── Points Module ───────────────────────────────────────────────

  describe('Points API Contract (Requirements 6.1-6.5)', () => {
    it('POST /points/add sends userId, pointsType, amount, description', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { id: 1, userId: 10, pointsType: 'COMMUNITY_ACTIVITY', amount: 50, description: '月度活跃', createdAt: '2024-06-01T10:00:00' },
      })

      const req: AddPointsRequest = { userId: 10, pointsType: 'COMMUNITY_ACTIVITY', amount: 50, description: '月度活跃' }
      const res = await addPoints(req)

      expect(mockPost).toHaveBeenCalledWith('/points/add', req)
      expect(res.data.userId).toBe(10)
      expect(res.data.pointsType).toBe('COMMUNITY_ACTIVITY')
      expect(res.data.amount).toBe(50)
    })

    it('POST /points/deduct sends userId, pointsType, amount, description', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { id: 2, userId: 10, pointsType: 'CHECKIN', amount: -20, description: '签到不足', createdAt: '2024-06-01T10:00:00' },
      })

      const req: DeductPointsRequest = { userId: 10, pointsType: 'CHECKIN', amount: 20, description: '签到不足' }
      const res = await deductPoints(req)

      expect(mockPost).toHaveBeenCalledWith('/points/deduct', req)
      expect(res.data.amount).toBe(-20)
    })

    it('GET /points/records/{userId} returns points history for a user', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 1, userId: 10, pointsType: 'COMMUNITY_ACTIVITY', amount: 50, description: null, createdAt: '2024-06-01T10:00:00' },
          { id: 2, userId: 10, pointsType: 'CHECKIN', amount: -20, description: '签到不足', createdAt: '2024-06-02T10:00:00' },
        ],
      })

      const res = await getPointsRecords(10)

      expect(mockGet).toHaveBeenCalledWith('/points/records/10')
      expect(res.data).toHaveLength(2)
      expect(res.data[0].pointsType).toBe('COMMUNITY_ACTIVITY')
    })

    it('GET /points/total/{userId} returns total points as number', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: 130,
      })

      const res = await getTotalPoints(10)

      expect(mockGet).toHaveBeenCalledWith('/points/total/10')
      expect(res.data).toBe(130)
    })

    it('supports all 8 points types matching backend PointsType enum', async () => {
      const pointsTypes = [
        'COMMUNITY_ACTIVITY',
        'CHECKIN',
        'VIOLATION_HANDLING',
        'TASK_COMPLETION',
        'ANNOUNCEMENT',
        'EVENT_HOSTING',
        'BIRTHDAY_BONUS',
        'MONTHLY_EXCELLENT',
      ]

      for (const type of pointsTypes) {
        mockPost.mockResolvedValueOnce({
          code: 200,
          message: 'success',
          data: { id: 1, userId: 10, pointsType: type, amount: 10, description: null, createdAt: '2024-06-01T10:00:00' },
        })

        const res = await addPoints({ userId: 10, pointsType: type, amount: 10 })
        expect(res.data.pointsType).toBe(type)
      }

      expect(mockPost).toHaveBeenCalledTimes(8)
    })
  })

  // ─── Salary Module ───────────────────────────────────────────────

  describe('Salary API Contract (Requirements 7.1-7.12)', () => {
    const mockSalaryRecords: SalaryRecord[] = [
      { id: 1, userId: 10, basePoints: 80, bonusPoints: 20, deductions: 0, totalPoints: 100, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
      { id: 2, userId: 11, basePoints: 70, bonusPoints: 30, deductions: 5, totalPoints: 95, miniCoins: 350, salaryAmount: 350, remark: null, version: 1, archived: false },
      { id: 3, userId: 12, basePoints: 60, bonusPoints: 10, deductions: 0, totalPoints: 70, miniCoins: 350, salaryAmount: 350, remark: null, version: 1, archived: false },
      { id: 4, userId: 13, basePoints: 90, bonusPoints: 15, deductions: 10, totalPoints: 95, miniCoins: 400, salaryAmount: 400, remark: null, version: 1, archived: false },
      { id: 5, userId: 14, basePoints: 50, bonusPoints: 25, deductions: 0, totalPoints: 75, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
    ]

    it('GET /salary/list returns salary records with all required fields', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: mockSalaryRecords,
      })

      const res = await getSalaryList()

      expect(mockGet).toHaveBeenCalledWith('/salary/list')
      expect(res.data).toHaveLength(5)
      // Verify all fields present
      const record = res.data[0]
      expect(record).toHaveProperty('id')
      expect(record).toHaveProperty('userId')
      expect(record).toHaveProperty('basePoints')
      expect(record).toHaveProperty('bonusPoints')
      expect(record).toHaveProperty('deductions')
      expect(record).toHaveProperty('totalPoints')
      expect(record).toHaveProperty('miniCoins')
      expect(record).toHaveProperty('salaryAmount')
      expect(record).toHaveProperty('remark')
      expect(record).toHaveProperty('version')
      expect(record).toHaveProperty('archived')
    })

    it('POST /salary/batch-save sends records array and operatorId', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          success: true,
          savedRecords: mockSalaryRecords,
          errors: [],
          globalError: null,
          violatingUserIds: [],
        },
      })

      const req: BatchSaveRequest = {
        records: mockSalaryRecords,
        operatorId: 1,
      }
      const res = await batchSaveSalary(req)

      expect(mockPost).toHaveBeenCalledWith('/salary/batch-save', req)
      expect(res.data.success).toBe(true)
      expect(res.data.savedRecords).toHaveLength(5)
    })

    it('POST /salary/batch-save returns validation errors when miniCoins out of range', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          success: false,
          savedRecords: null,
          errors: [{ userId: 10, field: 'miniCoins', message: '迷你币不在 [200, 400] 范围内' }],
          globalError: null,
          violatingUserIds: [10],
        },
      })

      const badRecords = [{ ...mockSalaryRecords[0], miniCoins: 100 }]
      const res = await batchSaveSalary({ records: badRecords, operatorId: 1 })

      expect(res.data.success).toBe(false)
      expect(res.data.errors).toHaveLength(1)
      expect(res.data.violatingUserIds).toContain(10)
    })

    it('POST /salary/batch-save returns global error when total exceeds 2000', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          success: false,
          savedRecords: null,
          errors: [],
          globalError: '迷你币总额超过 2000 上限',
          violatingUserIds: [],
        },
      })

      const res = await batchSaveSalary({ records: mockSalaryRecords, operatorId: 1 })

      expect(res.data.success).toBe(false)
      expect(res.data.globalError).toContain('2000')
    })

    it('POST /salary/calculate triggers salary calculation', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: mockSalaryRecords,
      })

      const res = await calculateSalaries()

      expect(mockPost).toHaveBeenCalledWith('/salary/calculate')
      expect(res.data).toHaveLength(5)
    })

    it('POST /salary/archive sends operatorId in request body', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: 5,
      })

      const res = await archiveSalary(1)

      expect(mockPost).toHaveBeenCalledWith('/salary/archive', { operatorId: 1 })
      expect(res.data).toBe(5)
    })
  })

  // ─── Member Rotation Module ──────────────────────────────────────

  describe('Member Rotation API Contract (Requirements 8.1-8.4)', () => {
    it('POST /member-rotation/check-promotion returns eligible interns', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 20, username: 'intern1', role: 'INTERN', enabled: true, pendingDismissal: false, createdAt: '2024-01-01T00:00:00', updatedAt: '2024-06-01T00:00:00' },
        ],
      })

      const res = await checkPromotionEligibility()

      expect(mockPost).toHaveBeenCalledWith('/member-rotation/check-promotion')
      expect(res.data).toHaveLength(1)
      expect(res.data[0].role).toBe('INTERN')
    })

    it('POST /member-rotation/check-demotion returns underperforming members', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 14, username: 'member4', role: 'MEMBER', enabled: true, pendingDismissal: false, createdAt: '2024-01-01T00:00:00', updatedAt: '2024-06-01T00:00:00' },
        ],
      })

      const res = await checkDemotionCandidates()

      expect(mockPost).toHaveBeenCalledWith('/member-rotation/check-demotion')
      expect(res.data).toHaveLength(1)
      expect(res.data[0].role).toBe('MEMBER')
    })

    it('POST /member-rotation/trigger-review returns boolean', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: true,
      })

      const res = await triggerPromotionReview()

      expect(mockPost).toHaveBeenCalledWith('/member-rotation/trigger-review')
      expect(res.data).toBe(true)
    })

    it('POST /member-rotation/execute sends internId and memberId', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: null,
      })

      await executePromotion({ internId: 20, memberId: 14 })

      expect(mockPost).toHaveBeenCalledWith('/member-rotation/execute', {
        internId: 20,
        memberId: 14,
      })
    })

    it('POST /member-rotation/mark-dismissal returns marked members', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 21, username: 'intern2', role: 'INTERN', enabled: true, pendingDismissal: true, createdAt: '2024-01-01T00:00:00', updatedAt: '2024-06-01T00:00:00' },
        ],
      })

      const res = await markForDismissal()

      expect(mockPost).toHaveBeenCalledWith('/member-rotation/mark-dismissal')
      expect(res.data[0].pendingDismissal).toBe(true)
    })

    it('GET /member-rotation/pending-dismissal returns pending list', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 21, username: 'intern2', role: 'INTERN', enabled: true, pendingDismissal: true, createdAt: '2024-01-01T00:00:00', updatedAt: '2024-06-01T00:00:00' },
        ],
      })

      const res = await getPendingDismissalList()

      expect(mockGet).toHaveBeenCalledWith('/member-rotation/pending-dismissal')
      expect(res.data).toHaveLength(1)
    })
  })

  // ─── End-to-End Flow Sequences ───────────────────────────────────

  describe('Flow 1: Points → Salary Calculation → Batch Save → Archive', () => {
    it('complete salary flow calls APIs in correct order', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/points/add': {
            code: 200, message: 'success',
            data: { id: 1, userId: 10, pointsType: 'COMMUNITY_ACTIVITY', amount: 80, description: null, createdAt: '2024-06-01T10:00:00' },
          },
          '/points/deduct': {
            code: 200, message: 'success',
            data: { id: 2, userId: 10, pointsType: 'CHECKIN', amount: -10, description: '签到不足', createdAt: '2024-06-01T10:00:00' },
          },
          '/salary/calculate': {
            code: 200, message: 'success',
            data: [
              { id: 1, userId: 10, basePoints: 80, bonusPoints: 0, deductions: 10, totalPoints: 70, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
              { id: 2, userId: 11, basePoints: 70, bonusPoints: 0, deductions: 0, totalPoints: 70, miniCoins: 350, salaryAmount: 350, remark: null, version: 1, archived: false },
              { id: 3, userId: 12, basePoints: 60, bonusPoints: 0, deductions: 0, totalPoints: 60, miniCoins: 350, salaryAmount: 350, remark: null, version: 1, archived: false },
              { id: 4, userId: 13, basePoints: 90, bonusPoints: 0, deductions: 0, totalPoints: 90, miniCoins: 400, salaryAmount: 400, remark: null, version: 1, archived: false },
              { id: 5, userId: 14, basePoints: 50, bonusPoints: 0, deductions: 0, totalPoints: 50, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
            ],
          },
          '/salary/batch-save': {
            code: 200, message: 'success',
            data: { success: true, savedRecords: [], errors: [], globalError: null, violatingUserIds: [] },
          },
          '/salary/archive': {
            code: 200, message: 'success',
            data: 5,
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/points/records/10': {
            code: 200, message: 'success',
            data: [
              { id: 1, userId: 10, pointsType: 'COMMUNITY_ACTIVITY', amount: 80, description: null, createdAt: '2024-06-01T10:00:00' },
            ],
          },
          '/points/total/10': { code: 200, message: 'success', data: 70 },
          '/salary/list': {
            code: 200, message: 'success',
            data: [
              { id: 1, userId: 10, basePoints: 80, bonusPoints: 0, deductions: 10, totalPoints: 70, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
            ],
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      // Step 1: Add points for a member
      await addPoints({ userId: 10, pointsType: 'COMMUNITY_ACTIVITY', amount: 80 })
      // Step 2: Deduct points (checkin penalty)
      await deductPoints({ userId: 10, pointsType: 'CHECKIN', amount: 10, description: '签到不足' })
      // Step 3: View points records
      await getPointsRecords(10)
      // Step 4: Check total points
      await getTotalPoints(10)
      // Step 5: Calculate salaries (points → mini coins → salary)
      await calculateSalaries()
      // Step 6: View salary list
      await getSalaryList()
      // Step 7: Batch save with edits
      await batchSaveSalary({
        records: [{ id: 1, userId: 10, basePoints: 80, bonusPoints: 0, deductions: 10, totalPoints: 70, miniCoins: 300, salaryAmount: 300, remark: '已确认', version: 1, archived: false }],
        operatorId: 1,
      })
      // Step 8: Archive
      await archiveSalary(1)

      expect(callOrder).toEqual([
        'POST /points/add',
        'POST /points/deduct',
        'GET /points/records/10',
        'GET /points/total/10',
        'POST /salary/calculate',
        'GET /salary/list',
        'POST /salary/batch-save',
        'POST /salary/archive',
      ])
    })
  })

  describe('Flow 2: Points Monitoring → Promotion Review → Role Rotation', () => {
    it('complete rotation flow calls APIs in correct order', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/member-rotation/check-promotion': {
            code: 200, message: 'success',
            data: [{ id: 20, username: 'intern1', role: 'INTERN', enabled: true, pendingDismissal: false }],
          },
          '/member-rotation/check-demotion': {
            code: 200, message: 'success',
            data: [{ id: 14, username: 'member4', role: 'MEMBER', enabled: true, pendingDismissal: false }],
          },
          '/member-rotation/trigger-review': {
            code: 200, message: 'success',
            data: true,
          },
          '/member-rotation/execute': {
            code: 200, message: 'success',
            data: null,
          },
          '/member-rotation/mark-dismissal': {
            code: 200, message: 'success',
            data: [{ id: 21, username: 'intern2', role: 'INTERN', enabled: true, pendingDismissal: true }],
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/member-rotation/pending-dismissal': {
            code: 200, message: 'success',
            data: [{ id: 21, username: 'intern2', role: 'INTERN', enabled: true, pendingDismissal: true }],
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      // Step 1: Check promotion eligibility (intern with 100+ points)
      const eligRes = await checkPromotionEligibility()
      expect(eligRes.data).toHaveLength(1)

      // Step 2: Check demotion candidates (member with <150 points for 2 months)
      const demRes = await checkDemotionCandidates()
      expect(demRes.data).toHaveLength(1)

      // Step 3: Trigger promotion review
      const triggerRes = await triggerPromotionReview()
      expect(triggerRes.data).toBe(true)

      // Step 4: Execute promotion (swap intern → member, member → intern)
      await executePromotion({ internId: 20, memberId: 14 })

      // Step 5: Mark underperforming interns for dismissal
      const markRes = await markForDismissal()
      expect(markRes.data[0].pendingDismissal).toBe(true)

      // Step 6: View pending dismissal list
      const pendingRes = await getPendingDismissalList()
      expect(pendingRes.data).toHaveLength(1)

      expect(callOrder).toEqual([
        'POST /member-rotation/check-promotion',
        'POST /member-rotation/check-demotion',
        'POST /member-rotation/trigger-review',
        'POST /member-rotation/execute',
        'POST /member-rotation/mark-dismissal',
        'GET /member-rotation/pending-dismissal',
      ])
    })
  })

  // ─── Cross-Module Integration ────────────────────────────────────

  describe('Cross-Module: Points → Salary → Rotation Integration', () => {
    it('points accumulation feeds into salary calculation which feeds into rotation checks', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string, data?: unknown) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/points/add': { code: 200, message: 'success', data: { id: 1, userId: 20, pointsType: 'COMMUNITY_ACTIVITY', amount: 100, description: null } },
          '/salary/calculate': {
            code: 200, message: 'success',
            data: [
              { id: 1, userId: 10, basePoints: 80, bonusPoints: 20, deductions: 0, totalPoints: 100, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
              { id: 2, userId: 11, basePoints: 70, bonusPoints: 10, deductions: 0, totalPoints: 80, miniCoins: 350, salaryAmount: 350, remark: null, version: 1, archived: false },
              { id: 3, userId: 12, basePoints: 60, bonusPoints: 10, deductions: 0, totalPoints: 70, miniCoins: 350, salaryAmount: 350, remark: null, version: 1, archived: false },
              { id: 4, userId: 13, basePoints: 50, bonusPoints: 5, deductions: 0, totalPoints: 55, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
              { id: 5, userId: 14, basePoints: 40, bonusPoints: 5, deductions: 0, totalPoints: 45, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false },
            ],
          },
          '/salary/batch-save': { code: 200, message: 'success', data: { success: true, savedRecords: [], errors: [], globalError: null, violatingUserIds: [] } },
          '/member-rotation/check-promotion': { code: 200, message: 'success', data: [{ id: 20, username: 'intern1', role: 'INTERN', enabled: true, pendingDismissal: false }] },
          '/member-rotation/check-demotion': { code: 200, message: 'success', data: [{ id: 14, username: 'member4', role: 'MEMBER', enabled: true, pendingDismissal: false }] },
          '/member-rotation/execute': { code: 200, message: 'success', data: null },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/points/total/20': { code: 200, message: 'success', data: 100 },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      // Phase 1: Points accumulation for intern
      await addPoints({ userId: 20, pointsType: 'COMMUNITY_ACTIVITY', amount: 100 })
      const totalRes = await getTotalPoints(20)
      expect(totalRes.data).toBe(100) // Intern reached 100 points threshold

      // Phase 2: Salary calculation for formal members
      await calculateSalaries()
      await batchSaveSalary({
        records: [{ id: 5, userId: 14, basePoints: 40, bonusPoints: 5, deductions: 0, totalPoints: 45, miniCoins: 300, salaryAmount: 300, remark: null, version: 1, archived: false }],
        operatorId: 1,
      })

      // Phase 3: Rotation check - intern qualifies, member underperforms
      const eligRes = await checkPromotionEligibility()
      expect(eligRes.data).toHaveLength(1)
      const demRes = await checkDemotionCandidates()
      expect(demRes.data).toHaveLength(1)

      // Phase 4: Execute rotation
      await executePromotion({ internId: 20, memberId: 14 })

      expect(callOrder).toEqual([
        'POST /points/add',
        'GET /points/total/20',
        'POST /salary/calculate',
        'POST /salary/batch-save',
        'POST /member-rotation/check-promotion',
        'POST /member-rotation/check-demotion',
        'POST /member-rotation/execute',
      ])
    })
  })
})
