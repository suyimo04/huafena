import http from './axios'
import type { ApiResponse } from './axios'

export interface ActivityDTO {
  id: number
  name: string
  description: string | null
  activityTime: string
  location: string
  registrationCount: number
  status: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'ARCHIVED'
  createdBy: number
  createdAt: string
}

export interface ActivityRegistrationDTO {
  id: number
  activityId: number
  userId: number
  checkedIn: boolean
  checkedInAt: string | null
  registeredAt: string
}

export interface CreateActivityRequest {
  name: string
  description?: string
  eventTime: string
  location: string
  createdBy: number
}

export function listActivities(): Promise<ApiResponse<ActivityDTO[]>> {
  return http.get('/activities')
}

export function createActivity(data: CreateActivityRequest): Promise<ApiResponse<ActivityDTO>> {
  return http.post('/activities', data)
}

export function registerForActivity(activityId: number, userId: number): Promise<ApiResponse<ActivityRegistrationDTO>> {
  return http.post(`/activities/${activityId}/register`, null, { params: { userId } })
}

export function checkInActivity(activityId: number, userId: number): Promise<ApiResponse<ActivityRegistrationDTO>> {
  return http.post(`/activities/${activityId}/check-in`, null, { params: { userId } })
}

export function archiveActivity(activityId: number): Promise<ApiResponse<ActivityDTO>> {
  return http.post(`/activities/${activityId}/archive`)
}
