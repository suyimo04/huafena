import http from './axios'
import type { ApiResponse } from './axios'

export interface SalaryRecord {
  id: number
  userId: number
  basePoints: number
  bonusPoints: number
  deductions: number
  totalPoints: number
  miniCoins: number
  salaryAmount: number
  remark: string | null
  version: number
  archived: boolean
}

export interface BatchSaveRequest {
  records: SalaryRecord[]
  operatorId: number
}

export interface ValidationError {
  userId: number
  field: string
  message: string
}

export interface BatchSaveResponse {
  success: boolean
  savedRecords: SalaryRecord[] | null
  errors: ValidationError[]
  globalError: string | null
  violatingUserIds: number[]
}

export function getSalaryList(): Promise<ApiResponse<SalaryRecord[]>> {
  return http.get('/salary/list')
}

export function updateSalaryRecord(id: number, data: Partial<SalaryRecord>): Promise<ApiResponse<SalaryRecord>> {
  return http.put(`/salary/${id}`, data)
}

export function batchSaveSalary(data: BatchSaveRequest): Promise<ApiResponse<BatchSaveResponse>> {
  return http.post('/salary/batch-save', data)
}

export function calculateSalaries(): Promise<ApiResponse<SalaryRecord[]>> {
  return http.post('/salary/calculate')
}

export function archiveSalary(operatorId: number): Promise<ApiResponse<number>> {
  return http.post('/salary/archive', { operatorId })
}
