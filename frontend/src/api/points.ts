import http from './axios'
import type { ApiResponse } from './axios'

export interface PointsRecordDTO {
  id: number
  userId: number
  pointsType: string
  amount: number
  description: string | null
  createdAt: string
}

export interface AddPointsRequest {
  userId: number
  pointsType: string
  amount: number
  description?: string
}

export interface DeductPointsRequest {
  userId: number
  pointsType: string
  amount: number
  description?: string
}

export function getPointsRecords(userId: number): Promise<ApiResponse<PointsRecordDTO[]>> {
  return http.get(`/points/records/${userId}`)
}

export function getTotalPoints(userId: number): Promise<ApiResponse<number>> {
  return http.get(`/points/total/${userId}`)
}

export function addPoints(data: AddPointsRequest): Promise<ApiResponse<PointsRecordDTO>> {
  return http.post('/points/add', data)
}

export function deductPoints(data: DeductPointsRequest): Promise<ApiResponse<PointsRecordDTO>> {
  return http.post('/points/deduct', data)
}
