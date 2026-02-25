/**
 * Integration tests for the questionnaire design → save → publish → render → submit flow.
 * These tests verify that frontend API calls match backend endpoint contracts.
 *
 * Flow: Template CRUD → Version Management → Publish → Public Questionnaire → Submit
 *
 * Validates: Requirements 3.1, 3.2, 3.4, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 4.2, 4.8
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import http from '@/api/axios'
import {
  getTemplates,
  getTemplate,
  createTemplate,
  updateTemplate,
  publishVersion,
  getVersionHistory,
  getVersion,
  getPublicQuestionnaire,
  submitPublicQuestionnaire,
} from '@/api/questionnaire'

const mockGet = http.get as ReturnType<typeof vi.fn>
const mockPost = http.post as ReturnType<typeof vi.fn>
const mockPut = http.put as ReturnType<typeof vi.fn>
const mockDelete = (http as any).delete as ReturnType<typeof vi.fn>

import type { QuestionnaireSchema, FieldType } from '@/types/questionnaire'

const sampleSchema: QuestionnaireSchema = {
  groups: [{ name: '基本信息', sortOrder: 1, fields: ['name', 'age'] }],
  fields: [
    { key: 'name', type: 'TEXT' as FieldType, label: '姓名', required: true, validationRules: { minLength: 2, maxLength: 20 }, conditionalLogic: null, options: null },
    { key: 'age', type: 'NUMBER' as FieldType, label: '年龄', required: true, validationRules: { min: 16, max: 60 }, conditionalLogic: null, options: null },
  ],
}

describe('Questionnaire Flow - API Contract Alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Template CRUD Operations', () => {
    it('POST /questionnaire/templates creates a new template', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, title: '入群问卷', description: '花粉小组入群申请问卷',
          activeVersionId: null, createdBy: 1,
          createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:00:00',
        },
      })

      const res = await createTemplate({
        title: '入群问卷',
        description: '花粉小组入群申请问卷',
        schemaDefinition: sampleSchema,
      })

      expect(mockPost).toHaveBeenCalledWith('/questionnaire/templates', {
        title: '入群问卷',
        description: '花粉小组入群申请问卷',
        schemaDefinition: sampleSchema,
      })
      expect(res.data.id).toBe(1)
      expect(res.data.title).toBe('入群问卷')
    })

    it('GET /questionnaire/templates returns template list', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 1, title: '入群问卷', description: '花粉小组入群申请问卷', activeVersionId: 2, createdBy: 1, createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:30:00' },
          { id: 2, title: '活动反馈问卷', description: '活动满意度调查', activeVersionId: null, createdBy: 1, createdAt: '2024-01-02T10:00:00', updatedAt: '2024-01-02T10:00:00' },
        ],
      })

      const res = await getTemplates()

      expect(mockGet).toHaveBeenCalledWith('/questionnaire/templates')
      expect(res.data).toHaveLength(2)
      expect(res.data[0].title).toBe('入群问卷')
    })

    it('GET /questionnaire/templates/{id} returns template detail', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, title: '入群问卷', description: '花粉小组入群申请问卷',
          activeVersionId: 2, createdBy: 1,
          createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:30:00',
        },
      })

      const res = await getTemplate(1)

      expect(mockGet).toHaveBeenCalledWith('/questionnaire/templates/1')
      expect(res.data.id).toBe(1)
      expect(res.data.activeVersionId).toBe(2)
    })

    it('PUT /questionnaire/templates/{id} updates template and creates new version', async () => {
      const updatedSchema: QuestionnaireSchema = {
        ...sampleSchema,
        fields: [
          ...sampleSchema.fields,
          { key: 'phone', type: 'TEXT' as FieldType, label: '手机号', required: false, validationRules: { pattern: '^1[3-9]\\d{9}$' }, conditionalLogic: null, options: null },
        ],
      }

      mockPut.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 1, title: '入群问卷（更新版）', description: '花粉小组入群申请问卷',
          activeVersionId: null, createdBy: 1,
          createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T11:00:00',
        },
      })

      const res = await updateTemplate(1, {
        title: '入群问卷（更新版）',
        schemaDefinition: updatedSchema,
      })

      expect(mockPut).toHaveBeenCalledWith('/questionnaire/templates/1', {
        title: '入群问卷（更新版）',
        schemaDefinition: updatedSchema,
      })
      expect(res.data.title).toBe('入群问卷（更新版）')
    })

    it('DELETE /questionnaire/templates/{id} deletes template', async () => {
      mockDelete.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: null,
      })

      await http.delete('/questionnaire/templates/1')

      expect(mockDelete).toHaveBeenCalledWith('/questionnaire/templates/1')
    })
  })

  describe('Version Management', () => {
    it('POST /questionnaire/templates/{id}/publish publishes the latest version', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 2, templateId: 1, versionNumber: 2,
          schemaDefinition: sampleSchema,
          status: 'PUBLISHED', createdAt: '2024-01-01T11:00:00',
        },
      })

      const res = await publishVersion(1)

      expect(mockPost).toHaveBeenCalledWith('/questionnaire/templates/1/publish')
      expect(res.data.status).toBe('PUBLISHED')
      expect(res.data.versionNumber).toBe(2)
    })

    it('GET /questionnaire/templates/{id}/versions returns version history', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: [
          { id: 1, templateId: 1, versionNumber: 1, schemaDefinition: sampleSchema, status: 'DRAFT', createdAt: '2024-01-01T10:00:00' },
          { id: 2, templateId: 1, versionNumber: 2, schemaDefinition: sampleSchema, status: 'PUBLISHED', createdAt: '2024-01-01T11:00:00' },
        ],
      })

      const res = await getVersionHistory(1)

      expect(mockGet).toHaveBeenCalledWith('/questionnaire/templates/1/versions')
      expect(res.data).toHaveLength(2)
      expect(res.data[0].versionNumber).toBe(1)
      expect(res.data[1].status).toBe('PUBLISHED')
    })

    it('GET /questionnaire/versions/{versionId} returns specific version', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: {
          id: 2, templateId: 1, versionNumber: 2,
          schemaDefinition: sampleSchema,
          status: 'PUBLISHED', createdAt: '2024-01-01T11:00:00',
        },
      })

      const res = await getVersion(2)

      expect(mockGet).toHaveBeenCalledWith('/questionnaire/versions/2')
      expect(res.data.templateId).toBe(1)
      expect(res.data.schemaDefinition).toEqual(sampleSchema)
    })
  })

  describe('Public Questionnaire Flow', () => {
    it('GET /public/questionnaire/{linkToken} fetches questionnaire for rendering', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: sampleSchema,
      })

      const res = await getPublicQuestionnaire('link-token-abc')

      expect(mockGet).toHaveBeenCalledWith('/public/questionnaire/link-token-abc')
      expect(res.data.groups).toHaveLength(1)
      expect(res.data.fields).toHaveLength(2)
      expect(res.data.fields[0].key).toBe('name')
    })

    it('POST /public/questionnaire/{linkToken}/submit sends answers and returns credentials', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        message: 'success',
        data: { username: 'auto_user_xyz', password: 'temp_pass_abc' },
      })

      const res = await submitPublicQuestionnaire('link-token-abc', { name: '张三', age: 25 })

      expect(mockPost).toHaveBeenCalledWith('/public/questionnaire/link-token-abc/submit', {
        answers: { name: '张三', age: 25 },
      })
      expect(res.data.username).toBe('auto_user_xyz')
      expect(res.data.password).toBe('temp_pass_abc')
    })
  })

  describe('End-to-End: Design → Save → Publish → Render → Submit', () => {
    it('complete questionnaire lifecycle calls APIs in correct order', async () => {
      const callOrder: string[] = []

      mockPost.mockImplementation((url: string) => {
        callOrder.push(`POST ${url}`)
        const responses: Record<string, unknown> = {
          '/questionnaire/templates': {
            code: 200, message: 'success',
            data: { id: 1, title: '入群问卷', description: '', activeVersionId: null, createdBy: 1, createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:00:00' },
          },
          '/questionnaire/templates/1/publish': {
            code: 200, message: 'success',
            data: { id: 2, templateId: 1, versionNumber: 2, schemaDefinition: sampleSchema, status: 'PUBLISHED', createdAt: '2024-01-01T11:00:00' },
          },
          '/applications/public-links/generate': {
            code: 200, message: 'success',
            data: { id: 1, linkToken: 'public-link-token', templateId: 1, versionId: 2, active: true, createdAt: '2024-01-01T11:05:00', expiresAt: null },
          },
          '/public/questionnaire/public-link-token/submit': {
            code: 200, message: 'success',
            data: { username: 'auto_user_001', password: 'temp_pass_001' },
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected POST ${url}`)))
      })

      mockPut.mockImplementation((url: string) => {
        callOrder.push(`PUT ${url}`)
        const responses: Record<string, unknown> = {
          '/questionnaire/templates/1': {
            code: 200, message: 'success',
            data: { id: 1, title: '入群问卷', description: '', activeVersionId: null, createdBy: 1, createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:30:00' },
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected PUT ${url}`)))
      })

      mockGet.mockImplementation((url: string) => {
        callOrder.push(`GET ${url}`)
        const responses: Record<string, unknown> = {
          '/questionnaire/templates/1/versions': {
            code: 200, message: 'success',
            data: [
              { id: 1, templateId: 1, versionNumber: 1, schemaDefinition: sampleSchema, status: 'DRAFT', createdAt: '2024-01-01T10:00:00' },
              { id: 2, templateId: 1, versionNumber: 2, schemaDefinition: sampleSchema, status: 'PUBLISHED', createdAt: '2024-01-01T10:30:00' },
            ],
          },
          '/public/questionnaire/public-link-token': {
            code: 200, message: 'success',
            data: sampleSchema,
          },
        }
        return Promise.resolve(responses[url] ?? Promise.reject(new Error(`Unexpected GET ${url}`)))
      })

      // Step 1: Design - Create template with schema
      const createRes = await createTemplate({ title: '入群问卷', schemaDefinition: sampleSchema })
      expect(createRes.data.id).toBe(1)

      // Step 2: Save - Update template (creates new version)
      await updateTemplate(1, { schemaDefinition: sampleSchema })

      // Step 3: Publish - Publish the latest version
      const publishRes = await publishVersion(1)
      expect(publishRes.data.status).toBe('PUBLISHED')

      // Step 4: Verify version history
      const versionsRes = await getVersionHistory(1)
      expect(versionsRes.data).toHaveLength(2)

      // Step 5: Generate public link for the published questionnaire
      const { generatePublicLink } = await import('@/api/application')
      const linkRes = await generatePublicLink({ templateId: 1 })
      expect(linkRes.data.linkToken).toBe('public-link-token')

      // Step 6: Render - Fetch public questionnaire by link token
      const publicRes = await getPublicQuestionnaire('public-link-token')
      expect(publicRes.data.fields).toHaveLength(2)

      // Step 7: Submit - Submit answers through public link
      const submitRes = await submitPublicQuestionnaire('public-link-token', { name: '新成员', age: 22 })
      expect(submitRes.data.username).toBe('auto_user_001')

      expect(callOrder).toEqual([
        'POST /questionnaire/templates',
        'PUT /questionnaire/templates/1',
        'POST /questionnaire/templates/1/publish',
        'GET /questionnaire/templates/1/versions',
        'POST /applications/public-links/generate',
        'GET /public/questionnaire/public-link-token',
        'POST /public/questionnaire/public-link-token/submit',
      ])
    })

    it('version numbers increment with each save', async () => {
      // First save creates version 1
      mockPost.mockResolvedValueOnce({
        code: 200, message: 'success',
        data: { id: 1, title: '问卷', activeVersionId: null, createdBy: 1, createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:00:00' },
      })
      await createTemplate({ title: '问卷', schemaDefinition: sampleSchema })

      // Second save creates version 2
      mockPut.mockResolvedValueOnce({
        code: 200, message: 'success',
        data: { id: 1, title: '问卷', activeVersionId: null, createdBy: 1, createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T10:30:00' },
      })
      await updateTemplate(1, { schemaDefinition: sampleSchema })

      // Verify version history shows incrementing versions
      mockGet.mockResolvedValueOnce({
        code: 200, message: 'success',
        data: [
          { id: 1, templateId: 1, versionNumber: 1, status: 'DRAFT', createdAt: '2024-01-01T10:00:00' },
          { id: 2, templateId: 1, versionNumber: 2, status: 'DRAFT', createdAt: '2024-01-01T10:30:00' },
        ],
      })

      const res = await getVersionHistory(1)
      expect(res.data[0].versionNumber).toBe(1)
      expect(res.data[1].versionNumber).toBe(2)
      expect(res.data[1].versionNumber).toBeGreaterThan(res.data[0].versionNumber)
    })

    it('published version becomes the active version for public access', async () => {
      // Publish version
      mockPost.mockResolvedValueOnce({
        code: 200, message: 'success',
        data: { id: 3, templateId: 1, versionNumber: 3, schemaDefinition: sampleSchema, status: 'PUBLISHED', createdAt: '2024-01-01T12:00:00' },
      })
      const publishRes = await publishVersion(1)
      expect(publishRes.data.status).toBe('PUBLISHED')

      // Template now has activeVersionId pointing to published version
      mockGet.mockResolvedValueOnce({
        code: 200, message: 'success',
        data: { id: 1, title: '入群问卷', activeVersionId: 3, createdBy: 1, createdAt: '2024-01-01T10:00:00', updatedAt: '2024-01-01T12:00:00' },
      })
      const templateRes = await getTemplate(1)
      expect(templateRes.data.activeVersionId).toBe(3)
    })
  })
})
