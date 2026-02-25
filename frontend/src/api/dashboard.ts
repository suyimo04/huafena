import http from './axios'
import type { ApiResponse } from './axios'

export interface DashboardStatsDTO {
  totalMembers: number
  adminCount: number
  leaderCount: number
  viceLeaderCount: number
  memberCount: number
  internCount: number
  applicantCount: number
  totalActivities: number
  totalPointsRecords: number
}

export interface AuditLogDTO {
  id: number
  operatorId: number
  operationType: string
  operationTime: string
  operationDetail: string
}

export function getDashboardStats(): Promise<ApiResponse<DashboardStatsDTO>> {
  return http.get('/dashboard/stats')
}

export function getAuditLogs(operationType?: string): Promise<ApiResponse<AuditLogDTO[]>> {
  const params: Record<string, string> = {}
  if (operationType) {
    params.type = operationType
  }
  return http.get('/dashboard/audit-logs', { params })
}
