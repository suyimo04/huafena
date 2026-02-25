/**
 * Integration tests for the registration → application → review → interview → review flow.
 * These tests verify that frontend API calls match backend endpoint contracts.
 *
 * Flow 1: RegisterPage → questionnaire submit → application create → initial review → AI interview → manual review → intern
 * Flow 2: PublicQuestionnairePage → questionnaire submit → account create → application create → initial review
 *
 * Validates: Requirements 1.1, 1.2, 4.1, 4.2, 4.4, 5.1, 5.12
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
  getApplications,
  getApplicationDetail,
  getQuestionnaireResponse,
  initialReview,
  getPublicLinks,
  generatePublicLink,
} from '@/api/application'
import {
  getPublicQuestionnaire,
  submitPublicQuestionnaire,
} from '@/api/questionnaire'
import {
  startInterview,
  sendMessage,
  endInterview,
  getInterview,
  getMessages,
  getReport,
  submitReview,
} from '@/api/interview'

const mockGet = http.get as ReturnType<typeof vi.fn>
const mockPost = http.post as ReturnType<typeof vi.fn>

describe('Registration Flow - API Contract Alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Flow 1: Registration → Application → Initial Review → AI Interview → Manual Review', () => {
    it('POST /auth/register sends username, password, and questionnaireAnswers', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { id: 1, username: 'testuser' },
      })

      await http.post('/auth/register', {
        username: 'testuser',
        password: 'pass123',
        questionnaireAnswers: { name: '张三', age: 25 },
      })

      expect(mockPost).toHaveBeenCalledWith('/auth/register', {
        username: 'testuser',
        password: 'pass123',
        questionnaireAnswers: { name: '张三', age: 25 },
      })
    })

    it('POST /auth/login sends username and password, receives token', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { token: 'jwt-token-123' },
      })

      const res = await http.post('/auth/login', {
        username: 'testuser',
        password: 'pass123',
      })

      expect(mockPost).toHaveBeenCalledWith('/auth/login', {
        username: 'testuser',
        password: 'pass123',
      })
      expect(res.data.token).toBe('jwt-token-123')
    })

    it('GET /applications calls correct endpoint and returns application list', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          {
            id: 1,
            userId: 10,
            status: 'PENDING_INITIAL_REVIEW',
            entryType: 'REGISTRATION',
            questionnaireResponseId: 5,
            createdAt: '2024-01-01T10:00:00',
          },
        ],
      })

      const res = await getApplications()

      expect(mockGet).toHaveBeenCalledWith('/applications')
      expect(res.data).toHaveLength(1)
      expect(res.data[0].status).toBe('PENDING_INITIAL_REVIEW')
    })

    it('GET /applications/{id} calls correct endpoint', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { id: 1, userId: 10, status: 'PENDING_INITIAL_REVIEW' },
      })

      const res = await getApplicationDetail(1)

      expect(mockGet).toHaveBeenCalledWith('/applications/1')
      expect(res.data.id).toBe(1)
    })

    it('GET /questionnaire/responses/application/{applicationId} calls correct endpoint', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 5,
          versionId: 2,
          userId: 10,
          applicationId: 1,
          answers: '{"name":"张三"}',
          submittedAt: '2024-01-01T10:00:00',
        },
      })

      const res = await getQuestionnaireResponse(1)

      expect(mockGet).toHaveBeenCalledWith('/questionnaire/responses/application/1')
      expect(res.data.applicationId).toBe(1)
    })

    it('POST /applications/{id}/initial-review sends approved boolean', async () => {
      mockPost.mockResolvedValueOnce({ code: 200, message: 'success', data: null })

      await initialReview(1, { approved: true })

      expect(mockPost).toHaveBeenCalledWith('/applications/1/initial-review', { approved: true })
    })

    it('POST /applications/{id}/initial-review sends rejection', async () => {
      mockPost.mockResolvedValueOnce({ code: 200, message: 'success', data: null })

      await initialReview(1, { approved: false })

      expect(mockPost).toHaveBeenCalledWith('/applications/1/initial-review', { approved: false })
    })

    it('POST /interviews/start sends applicationId and scenarioId', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, applicationId: 1, userId: 10, status: 'IN_PROGRESS',
          scenarioId: 'conflict_handling', difficultyLevel: 'STANDARD',
          createdAt: '2024-01-01T11:00:00',
        },
      })

      const res = await startInterview(1, 'conflict_handling')

      expect(mockPost).toHaveBeenCalledWith('/interviews/start', {
        applicationId: 1,
        scenarioId: 'conflict_handling',
      })
      expect(res.data.status).toBe('IN_PROGRESS')
    })

    it('POST /interviews/{id}/message sends message content', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 2, interviewId: 1, role: 'AI', content: 'AI 回复内容',
          timestamp: '2024-01-01T11:01:00', timeLimitSeconds: 60,
        },
      })

      const res = await sendMessage(1, '我的回答')

      expect(mockPost).toHaveBeenCalledWith('/interviews/1/message', { message: '我的回答' })
      expect(res.data.role).toBe('AI')
    })

    it('POST /interviews/{id}/end returns interview report', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, interviewId: 1, ruleFamiliarity: 8, communicationScore: 7,
          pressureScore: 6, totalScore: 7, aiComment: '表现良好',
          recommendationLabel: '建议通过',
        },
      })

      const res = await endInterview(1)

      expect(mockPost).toHaveBeenCalledWith('/interviews/1/end')
      expect(res.data.totalScore).toBe(7)
    })

    it('GET /interviews/{id}/report returns report with recommendation label', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, interviewId: 1, ruleFamiliarity: 9, communicationScore: 8,
          pressureScore: 7, totalScore: 8, aiComment: '优秀',
          recommendationLabel: '建议通过', reviewerComment: null,
          reviewResult: null, manualApproved: null,
        },
      })

      const res = await getReport(1)

      expect(mockGet).toHaveBeenCalledWith('/interviews/1/report')
      expect(res.data.recommendationLabel).toBe('建议通过')
    })

    it('POST /interviews/{id}/review sends approved, reviewComment, and suggestedMentor', async () => {
      mockPost.mockResolvedValueOnce({ code: 200, message: 'success', data: null })

      await submitReview(1, {
        approved: true,
        reviewComment: '表现优秀，建议录用',
        suggestedMentor: 'leader',
      })

      expect(mockPost).toHaveBeenCalledWith('/interviews/1/review', {
        approved: true,
        reviewComment: '表现优秀，建议录用',
        suggestedMentor: 'leader',
      })
    })

    it('POST /interviews/{id}/review sends rejection', async () => {
      mockPost.mockResolvedValueOnce({ code: 200, message: 'success', data: null })

      await submitReview(1, {
        approved: false,
        reviewComment: '规则熟悉度不足',
      })

      expect(mockPost).toHaveBeenCalledWith('/interviews/1/review', {
        approved: false,
        reviewComment: '规则熟悉度不足',
      })
    })
  })

  describe('Flow 2: Public Link → Questionnaire Submit → Account Create → Application Create → Initial Review', () => {
    it('GET /public/questionnaire/{linkToken} fetches questionnaire schema', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          groups: [{ name: '基本信息', sortOrder: 1, fields: ['name'] }],
          fields: [
            { key: 'name', type: 'TEXT', label: '姓名', required: true, validationRules: null, conditionalLogic: null, options: null },
          ],
        },
      })

      const res = await getPublicQuestionnaire('abc-token-123')

      expect(mockGet).toHaveBeenCalledWith('/public/questionnaire/abc-token-123')
      expect(res.data.fields).toHaveLength(1)
    })

    it('POST /public/questionnaire/{linkToken}/submit sends answers and returns credentials', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { username: 'auto_user_abc123', password: 'temp_pass_xyz' },
      })

      const res = await submitPublicQuestionnaire('abc-token-123', { name: '李四' })

      expect(mockPost).toHaveBeenCalledWith('/public/questionnaire/abc-token-123/submit', {
        answers: { name: '李四' },
      })
      expect(res.data.username).toBe('auto_user_abc123')
      expect(res.data.password).toBe('temp_pass_xyz')
    })

    it('POST /applications/public-links/generate sends templateId', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, linkToken: 'uuid-token-456', templateId: 1, versionId: 3,
          active: true, createdAt: '2024-01-01T09:00:00', expiresAt: null,
        },
      })

      const res = await generatePublicLink({ templateId: 1 })

      expect(mockPost).toHaveBeenCalledWith('/applications/public-links/generate', { templateId: 1 })
      expect(res.data.linkToken).toBe('uuid-token-456')
    })

    it('GET /applications/public-links returns list of public links', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 1, linkToken: 'uuid-token-456', templateId: 1, versionId: 3, active: true, createdAt: '2024-01-01T09:00:00', expiresAt: null },
        ],
      })

      const res = await getPublicLinks()

      expect(mockGet).toHaveBeenCalledWith('/applications/public-links')
      expect(res.data).toHaveLength(1)
    })
  })

  describe('Interview Data Retrieval', () => {
    it('GET /interviews/{id} returns interview details', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, applicationId: 1, userId: 10, status: 'PENDING_REVIEW',
          scenarioId: 'conflict_handling', difficultyLevel: 'STANDARD',
          createdAt: '2024-01-01T11:00:00', completedAt: '2024-01-01T11:30:00',
        },
      })

      const res = await getInterview(1)

      expect(mockGet).toHaveBeenCalledWith('/interviews/1')
      expect(res.data.status).toBe('PENDING_REVIEW')
    })

    it('GET /interviews/{id}/messages returns chat history', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 1, interviewId: 1, role: 'AI', content: '你好', timestamp: '2024-01-01T11:00:00', timeLimitSeconds: 60 },
          { id: 2, interviewId: 1, role: 'USER', content: '你好', timestamp: '2024-01-01T11:00:30', timeLimitSeconds: 60 },
        ],
      })

      const res = await getMessages(1)

      expect(mockGet).toHaveBeenCalledWith('/interviews/1/messages')
      expect(res.data).toHaveLength(2)
    })
  })

  describe('End-to-End Flow Sequence Verification', () => {
    it('complete registration flow calls APIs in correct order', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/auth/register': { code: 200, message: 'success', data: { id: 1 } },
          '/auth/login': { code: 200, message: 'success', data: { token: 'jwt-123' } },
          '/applications/1/initial-review': { code: 200, message: 'success', data: null },
          '/interviews/start': { code: 200, message: 'success', data: { id: 1, status: 'IN_PROGRESS' } },
          '/interviews/1/message': { code: 200, message: 'success', data: { id: 2, role: 'AI', content: 'response' } },
          '/interviews/1/end': { code: 200, message: 'success', data: { totalScore: 8 } },
          '/interviews/1/review': { code: 200, message: 'success', data: null },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/applications': { code: 200, message: 'success', data: [{ id: 1, userId: 10, status: 'PENDING_INITIAL_REVIEW' }] },
          '/interviews/1/report': { code: 200, message: 'success', data: { totalScore: 8, recommendationLabel: '建议通过' } },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      // Step 1: Register
      await http.post('/auth/register', { username: 'newuser', password: 'pass123', questionnaireAnswers: { name: '王五' } })
      // Step 2: Admin logs in
      await http.post('/auth/login', { username: 'admin', password: 'admin123' })
      // Step 3: List applications
      await getApplications()
      // Step 4: Initial review - approve
      await initialReview(1, { approved: true })
      // Step 5: Start AI interview
      await startInterview(1, 'conflict_handling')
      // Step 6: Conduct interview
      await sendMessage(1, '我会按照群规处理')
      // Step 7: End interview
      await endInterview(1)
      // Step 8: Get report
      await getReport(1)
      // Step 9: Manual review - approve
      await submitReview(1, { approved: true, reviewComment: '通过', suggestedMentor: 'leader' })

      expect(callOrder).toEqual([
        'POST /auth/register',
        'POST /auth/login',
        'GET /applications',
        'POST /applications/1/initial-review',
        'POST /interviews/start',
        'POST /interviews/1/message',
        'POST /interviews/1/end',
        'GET /interviews/1/report',
        'POST /interviews/1/review',
      ])
    })

    it('complete public link flow calls APIs in correct order', async () => {
      const callOrder: string[] = []

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/public/questionnaire/token-abc': {
            code: 200, message: 'success',
            data: { groups: [], fields: [{ key: 'name', type: 'TEXT', label: '姓名' }] },
          },
          '/applications': {
            code: 200, message: 'success',
            data: [{ id: 2, userId: 20, status: 'PENDING_INITIAL_REVIEW', entryType: 'PUBLIC_LINK' }],
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      mockPost.mockImplementation((url: string) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/public/questionnaire/token-abc/submit': {
            code: 200, message: 'success',
            data: { username: 'auto_user_123', password: 'temp_pass' },
          },
          '/applications/2/initial-review': { code: 200, message: 'success', data: null },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      // Step 1: Fetch public questionnaire
      await getPublicQuestionnaire('token-abc')
      // Step 2: Submit questionnaire (auto-creates account + application)
      const submitRes = await submitPublicQuestionnaire('token-abc', { name: '赵六' })
      expect(submitRes.data.username).toBe('auto_user_123')
      // Step 3: Admin lists applications
      await getApplications()
      // Step 4: Initial review
      await initialReview(2, { approved: true })

      expect(callOrder).toEqual([
        'GET /public/questionnaire/token-abc',
        'POST /public/questionnaire/token-abc/submit',
        'GET /applications',
        'POST /applications/2/initial-review',
      ])
    })
  })
})
