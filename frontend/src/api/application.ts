import http from './axios'
import type { ApiResponse } from './axios'

export interface ApplicationDTO {
  id: number
  userId: number
  status: ApplicationStatus
  entryType: 'REGISTRATION' | 'PUBLIC_LINK'
  questionnaireResponseId: number | null
  reviewComment: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
  // V3.1 fields
  pollenUid?: string | null
  birthDate?: string | null
  calculatedAge?: number | null
  educationStage?: string | null
  examFlag?: boolean
  examType?: string | null
  examDate?: string | null
  weeklyAvailableDays?: number | null
  dailyAvailableHours?: number | null
  screeningPassed?: boolean | null
  screeningRejectReason?: string | null
  needsAttention?: boolean
  attentionFlags?: string | null
}

export type ApplicationStatus =
  | 'PENDING_INITIAL_REVIEW'
  | 'INITIAL_REVIEW_PASSED'
  | 'REJECTED'
  | 'AUTO_REJECTED'
  | 'AI_INTERVIEW_IN_PROGRESS'
  | 'PENDING_REVIEW'
  | 'INTERN_OFFERED'
  | 'INTERNSHIP_IN_PROGRESS'
  | 'CONVERSION_REVIEW'
  | 'MEMBER_CONVERTED'

export interface QuestionnaireResponseDTO {
  id: number
  versionId: number
  userId: number
  applicationId: number | null
  answers: string
  submittedAt: string
}

export function getApplications(): Promise<ApiResponse<ApplicationDTO[]>> {
  return http.get('/applications')
}

export function getApplicationDetail(id: number): Promise<ApiResponse<ApplicationDTO>> {
  return http.get(`/applications/${id}`)
}

export function getQuestionnaireResponse(applicationId: number): Promise<ApiResponse<QuestionnaireResponseDTO>> {
  return http.get(`/questionnaire/responses/application/${applicationId}`)
}

export function initialReview(id: number, data: { approved: boolean; comment?: string }): Promise<ApiResponse<void>> {
  return http.post(`/applications/${id}/initial-review`, data)
}

export interface PublicLinkDTO {
  id: number
  linkToken: string
  templateId: number
  versionId: number
  active: boolean
  createdAt: string
  expiresAt: string | null
}

export function getPublicLinks(): Promise<ApiResponse<PublicLinkDTO[]>> {
  return http.get('/applications/public-links')
}

export function generatePublicLink(data: { templateId: number }): Promise<ApiResponse<PublicLinkDTO>> {
  return http.post('/applications/public-links/generate', data)
}

// V3.1: Batch operations
export function batchApprove(applicationIds: number[]): Promise<ApiResponse<void>> {
  return http.post('/applications/batch-approve', { applicationIds })
}

export function batchReject(applicationIds: number[]): Promise<ApiResponse<void>> {
  return http.post('/applications/batch-reject', { applicationIds })
}

export function batchNotifyInterview(applicationIds: number[]): Promise<ApiResponse<void>> {
  return http.post('/applications/batch-notify-interview', { applicationIds })
}

// V3.1: Excel export
export function exportApplicationsExcel(status?: string): Promise<Blob> {
  const params = status ? { status } : {}
  return http.get('/applications/export', {
    params,
    responseType: 'blob',
    // bypass the normal JSON response interceptor
  }) as any
}

// V3.1: Timeline
export interface TimelineEntry {
  id: number
  applicationId: number
  status: string
  operator: string
  description: string
  createdAt: string
}

export function getApplicationTimeline(id: number): Promise<ApiResponse<TimelineEntry[]>> {
  return http.get(`/applications/${id}/timeline`)
}
