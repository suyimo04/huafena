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

export interface RecruitmentStatsDTO {
  stageCount: Record<string, number>
  aiInterviewPassRate: number
  manualReviewPassRate: number
}

export interface MemberSalaryRank {
  userId: number
  username: string
  totalPoints: number
  miniCoins: number
}

export interface SalaryStatsDTO {
  totalPool: number
  allocated: number
  usageRate: number
  ranking: MemberSalaryRank[]
}

export interface MonthlyGrowthDTO {
  month: string
  count: number
}

export interface OperationsDataDTO {
  userGrowthTrend: MonthlyGrowthDTO[]
  issueProcessingStats: Record<string, number>
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

export function getRecruitmentStats(): Promise<ApiResponse<RecruitmentStatsDTO>> {
  return http.get('/dashboard/recruitment')
}

export function getSalaryStats(): Promise<ApiResponse<SalaryStatsDTO>> {
  return http.get('/dashboard/salary')
}

export function getOperationsData(): Promise<ApiResponse<OperationsDataDTO>> {
  return http.get('/dashboard/operations')
}
