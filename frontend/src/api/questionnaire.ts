import http from './axios'
import type { ApiResponse } from './axios'
import type { QuestionnaireSchema } from '@/types/questionnaire'

export interface TemplateDTO {
  id: number
  title: string
  description: string
  activeVersionId: number | null
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface VersionDTO {
  id: number
  templateId: number
  versionNumber: number
  schemaDefinition: QuestionnaireSchema
  status: 'DRAFT' | 'PUBLISHED'
  createdAt: string
}

export interface CreateTemplateRequest {
  title: string
  description?: string
  schemaDefinition: QuestionnaireSchema
}

export interface UpdateTemplateRequest {
  title?: string
  description?: string
  schemaDefinition: QuestionnaireSchema
}

export function getTemplates(): Promise<ApiResponse<TemplateDTO[]>> {
  return http.get('/questionnaire/templates')
}

export function getTemplate(id: number): Promise<ApiResponse<TemplateDTO>> {
  return http.get(`/questionnaire/templates/${id}`)
}

export function createTemplate(data: CreateTemplateRequest): Promise<ApiResponse<TemplateDTO>> {
  return http.post('/questionnaire/templates', data)
}

export function updateTemplate(id: number, data: UpdateTemplateRequest): Promise<ApiResponse<TemplateDTO>> {
  return http.put(`/questionnaire/templates/${id}`, data)
}

export function publishVersion(templateId: number): Promise<ApiResponse<VersionDTO>> {
  return http.post(`/questionnaire/templates/${templateId}/publish`)
}

export function getVersionHistory(templateId: number): Promise<ApiResponse<VersionDTO[]>> {
  return http.get(`/questionnaire/templates/${templateId}/versions`)
}

export function getVersion(versionId: number): Promise<ApiResponse<VersionDTO>> {
  return http.get(`/questionnaire/versions/${versionId}`)
}

export function getPublicQuestionnaire(linkToken: string): Promise<ApiResponse<QuestionnaireSchema>> {
  return http.get(`/public/questionnaire/${linkToken}`)
}

export function submitPublicQuestionnaire(
  linkToken: string,
  answers: Record<string, unknown>,
): Promise<ApiResponse<{ username: string; password: string }>> {
  return http.post(`/public/questionnaire/${linkToken}/submit`, { answers })
}
