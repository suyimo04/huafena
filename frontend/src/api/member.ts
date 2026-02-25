import http from './axios'
import type { ApiResponse } from './axios'

/* ---- Types ---- */

export type OnlineStatus = 'ONLINE' | 'BUSY' | 'OFFLINE'

export interface MemberCardItem {
  id: number
  username: string
  role: string
  onlineStatus: OnlineStatus
}

export interface WeeklyActivityHour {
  weekStart: string
  weekEnd: string
  totalMinutes: number
}

export interface RoleChangeRecord {
  id: number
  oldRole: string
  newRole: string
  changedBy: string
  changedAt: string
}

export interface MemberDetail {
  id: number
  username: string
  role: string
  onlineStatus: OnlineStatus
  lastActiveAt: string | null
  createdAt: string
  activityHours: WeeklyActivityHour[]
  roleHistory: RoleChangeRecord[]
}

/* ---- API Functions ---- */

export function listMembers(): Promise<ApiResponse<MemberCardItem[]>> {
  return http.get('/members')
}

export function getMemberDetail(id: number): Promise<ApiResponse<MemberDetail>> {
  return http.get(`/members/${id}`)
}

export function getActivityHours(id: number): Promise<ApiResponse<WeeklyActivityHour[]>> {
  return http.get(`/members/${id}/activity-hours`)
}

export function getRoleHistory(id: number): Promise<ApiResponse<RoleChangeRecord[]>> {
  return http.get(`/members/${id}/role-history`)
}

export function updateOnlineStatus(id: number, status: OnlineStatus): Promise<ApiResponse<void>> {
  return http.put(`/members/${id}/status`, { status })
}

export function heartbeat(id: number): Promise<ApiResponse<void>> {
  return http.post(`/members/${id}/heartbeat`)
}
