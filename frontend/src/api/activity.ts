import http from './axios'
import type { ApiResponse } from './axios'

/* ---- Types ---- */

export type ActivityType = 'ONLINE' | 'OFFLINE' | 'TRAINING' | 'TEAM_BUILDING' | 'OTHER'
export type ApprovalMode = 'AUTO' | 'MANUAL'
export type RegistrationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ActivityDTO {
  id: number
  name: string
  description: string | null
  coverImageUrl: string | null
  activityType: ActivityType | null
  customFormFields: string | null
  approvalMode: ApprovalMode
  activityTime: string
  location: string
  registrationCount: number
  status: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'ARCHIVED'
  qrToken: string | null
  createdBy: number
  createdAt: string
}

export interface ActivityRegistrationDTO {
  id: number
  activityId: number
  userId: number
  status: RegistrationStatus
  extraFields: string | null
  checkedIn: boolean
  checkedInAt: string | null
  registeredAt: string
}

export interface ActivityGroupDTO {
  id: number
  activityId: number
  groupName: string
  memberIds: string // JSON array
  createdAt: string
}

export interface ActivityFeedbackDTO {
  id: number
  activityId: number
  userId: number
  rating: number
  comment: string | null
  createdAt: string
}

export interface ActivityMaterialDTO {
  id: number
  activityId: number
  fileName: string
  fileUrl: string
  fileType: string | null
  uploadedBy: number
  uploadedAt: string
}

export interface ActivityStatisticsDTO {
  id: number
  activityId: number
  totalRegistered: number
  totalAttended: number
  checkInRate: number
  avgFeedbackRating: number
  feedbackSummary: string | null
  generatedAt: string
}

export interface CreateActivityRequest {
  name: string
  description?: string
  eventTime: string
  location: string
  createdBy: number
  coverImageUrl?: string
  activityType?: ActivityType
  customFormFields?: string
  approvalMode?: ApprovalMode
}

/* ---- API Functions ---- */

export function listActivities(): Promise<ApiResponse<ActivityDTO[]>> {
  return http.get('/activities')
}

export function createActivity(data: CreateActivityRequest): Promise<ApiResponse<ActivityDTO>> {
  return http.post('/activities', data)
}

export function registerForActivity(activityId: number, userId: number): Promise<ApiResponse<ActivityRegistrationDTO>> {
  return http.post(`/activities/${activityId}/register`, null, { params: { userId } })
}

export function checkInActivity(activityId: number, userId: number, qrToken?: string): Promise<ApiResponse<ActivityRegistrationDTO>> {
  const params: Record<string, any> = { userId }
  if (qrToken) params.qrToken = qrToken
  return http.post(`/activities/${activityId}/check-in`, null, { params })
}

export function archiveActivity(activityId: number): Promise<ApiResponse<ActivityDTO>> {
  return http.post(`/activities/${activityId}/archive`)
}

// Approval
export function approveRegistration(activityId: number, regId: number): Promise<ApiResponse<void>> {
  return http.post(`/activities/${activityId}/registrations/${regId}/approve`)
}

// Groups
export function createGroup(activityId: number, groupName: string, memberIds: number[]): Promise<ApiResponse<ActivityGroupDTO>> {
  return http.post(`/activities/${activityId}/groups`, { groupName, memberIds })
}

export function updateGroupMembers(activityId: number, groupId: number, memberIds: number[]): Promise<ApiResponse<void>> {
  return http.put(`/activities/${activityId}/groups/${groupId}`, { memberIds })
}

export function getGroups(activityId: number): Promise<ApiResponse<ActivityGroupDTO[]>> {
  return http.get(`/activities/${activityId}/groups`)
}

// QR Code
export function generateQrCode(activityId: number): Promise<ApiResponse<string>> {
  return http.get(`/activities/${activityId}/qr-code`)
}

// Feedback
export function submitFeedback(activityId: number, userId: number, rating: number, comment?: string): Promise<ApiResponse<void>> {
  return http.post(`/activities/${activityId}/feedback`, { rating, comment }, { params: { userId } })
}

export function getFeedback(activityId: number): Promise<ApiResponse<ActivityFeedbackDTO[]>> {
  return http.get(`/activities/${activityId}/feedback`)
}

// Statistics
export function getStatistics(activityId: number): Promise<ApiResponse<ActivityStatisticsDTO>> {
  return http.get(`/activities/${activityId}/statistics`)
}

// Materials
export function uploadMaterial(activityId: number, file: File, uploadedBy: number): Promise<ApiResponse<void>> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('uploadedBy', String(uploadedBy))
  return http.post(`/activities/${activityId}/materials`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getMaterials(activityId: number): Promise<ApiResponse<ActivityMaterialDTO[]>> {
  return http.get(`/activities/${activityId}/materials`)
}
