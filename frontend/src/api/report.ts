import http from './axios'
import type { ApiResponse } from './axios'

export interface WeeklyReportDTO {
  id: number
  weekStart: string
  weekEnd: string
  newApplications: number
  interviewsCompleted: number
  newMembers: number
  activitiesHeld: number
  totalPointsIssued: number
  detailData: string | null
  generatedAt: string
}

export function listWeeklyReports(): Promise<ApiResponse<WeeklyReportDTO[]>> {
  return http.get('/reports/weekly')
}

export function getWeeklyReport(id: number): Promise<ApiResponse<WeeklyReportDTO>> {
  return http.get(`/reports/weekly/${id}`)
}

export function generateWeeklyReport(weekStart?: string, weekEnd?: string): Promise<ApiResponse<WeeklyReportDTO>> {
  const params: Record<string, string> = {}
  if (weekStart) params.weekStart = weekStart
  if (weekEnd) params.weekEnd = weekEnd
  return http.post('/reports/weekly/generate', null, { params })
}

function downloadBlob(data: Blob, fileName: string) {
  const url = window.URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

async function downloadExport(url: string, fallbackName: string) {
  const token = localStorage.getItem('token')
  const response = await fetch(`/api${url}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) throw new Error('导出失败')
  const disposition = response.headers.get('Content-Disposition')
  const fileName = disposition?.match(/filename="?([^"]+)"?/)?.[1] ?? fallbackName
  const blob = await response.blob()
  downloadBlob(blob, fileName)
}

export function exportMembers() {
  return downloadExport('/reports/export/members', 'members.xlsx')
}

export function exportPoints() {
  return downloadExport('/reports/export/points', 'points.xlsx')
}

export function exportSalary() {
  return downloadExport('/reports/export/salary', 'salary.xlsx')
}

export function exportActivities() {
  return downloadExport('/reports/export/activities', 'activities.xlsx')
}

export function exportCustom(dataType: string, startDate: string, endDate: string) {
  return downloadExport(
    `/reports/export/custom?dataType=${encodeURIComponent(dataType)}&startDate=${startDate}&endDate=${endDate}`,
    `${dataType}_${startDate}_${endDate}.xlsx`,
  )
}
