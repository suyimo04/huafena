import http from './axios'
import type { ApiResponse } from './axios'

export interface MemberDTO {
  id: number
  username: string
  role: string
  enabled: boolean
  pendingDismissal: boolean
  createdAt: string
  updatedAt: string
}

export interface ExecutePromotionRequest {
  internId: number
  memberId: number
}

export function checkPromotionEligibility(): Promise<ApiResponse<MemberDTO[]>> {
  return http.post('/member-rotation/check-promotion')
}

export function checkDemotionCandidates(): Promise<ApiResponse<MemberDTO[]>> {
  return http.post('/member-rotation/check-demotion')
}

export function triggerPromotionReview(): Promise<ApiResponse<boolean>> {
  return http.post('/member-rotation/trigger-review')
}

export function executePromotion(data: ExecutePromotionRequest): Promise<ApiResponse<void>> {
  return http.post('/member-rotation/execute', data)
}

export function markForDismissal(): Promise<ApiResponse<MemberDTO[]>> {
  return http.post('/member-rotation/mark-dismissal')
}

export function getPendingDismissalList(): Promise<ApiResponse<MemberDTO[]>> {
  return http.get('/member-rotation/pending-dismissal')
}
