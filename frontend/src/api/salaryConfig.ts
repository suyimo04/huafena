import http from './axios'
import type { ApiResponse } from './axios'

/* ---- Types ---- */

export interface CheckinTier {
  minCount: number
  maxCount: number
  points: number
  label: string
}

export interface RotationThresholds {
  promotionPointsThreshold: number
  demotionSalaryThreshold: number
  demotionConsecutiveMonths: number
  dismissalPointsThreshold: number
  dismissalConsecutiveMonths: number
}

export type SalaryConfigMap = Record<string, string>

/* ---- API Functions ---- */

export function getSalaryConfig(): Promise<ApiResponse<SalaryConfigMap>> {
  return http.get('/salary-config')
}

export function updateSalaryConfig(config: SalaryConfigMap): Promise<ApiResponse<void>> {
  return http.put('/salary-config', config)
}

export function getCheckinTiers(): Promise<ApiResponse<CheckinTier[]>> {
  return http.get('/salary-config/checkin-tiers')
}

export function updateCheckinTiers(tiers: CheckinTier[]): Promise<ApiResponse<void>> {
  return http.put('/salary-config/checkin-tiers', tiers)
}
