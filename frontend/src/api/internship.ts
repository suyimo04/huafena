import http from './axios'
import type { ApiResponse } from './axios'

export interface InternshipDTO {
  id: number
  userId: number
  mentorId: number | null
  startDate: string
  expectedEndDate: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface InternshipListItemDTO {
  id: number
  userId: number
  username: string
  mentorName: string | null
  startDate: string
  expectedEndDate: string
  status: string
  taskCompletionRate: number
}

export interface InternshipTaskDTO {
  id: number
  internshipId: number
  taskName: string
  taskDescription: string | null
  deadline: string
  completed: boolean
  completedAt: string | null
  createdAt: string
}

export interface InternshipProgressDTO {
  taskCompletionRate: number
  totalPoints: number
  mentorComment: string | null
  remainingDays: number
  tasks: InternshipTaskDTO[]
}

export interface CreateTaskRequest {
  taskName: string
  taskDescription: string
  deadline: string
}

export function listInternships(status?: string): Promise<ApiResponse<InternshipListItemDTO[]>> {
  return http.get('/internships', { params: status ? { status } : {} })
}

export function getInternship(id: number): Promise<ApiResponse<InternshipDTO>> {
  return http.get(`/internships/${id}`)
}

export function createInternship(userId: number): Promise<ApiResponse<InternshipDTO>> {
  return http.post('/internships', { userId })
}

export function createTask(internshipId: number, data: CreateTaskRequest): Promise<ApiResponse<InternshipTaskDTO>> {
  return http.post(`/internships/${internshipId}/tasks`, data)
}

export function completeTask(internshipId: number, taskId: number): Promise<ApiResponse<void>> {
  return http.put(`/internships/${internshipId}/tasks/${taskId}`)
}

export function getTasks(internshipId: number): Promise<ApiResponse<InternshipTaskDTO[]>> {
  return http.get(`/internships/${internshipId}/tasks`)
}

export function assignMentor(internshipId: number, mentorId: number): Promise<ApiResponse<void>> {
  return http.put(`/internships/${internshipId}/mentor`, { mentorId })
}

export function getProgress(internshipId: number): Promise<ApiResponse<InternshipProgressDTO>> {
  return http.get(`/internships/${internshipId}/progress`)
}

export function approveConversion(internshipId: number): Promise<ApiResponse<void>> {
  return http.post(`/internships/${internshipId}/convert`)
}

export function extendInternship(internshipId: number, additionalDays: number): Promise<ApiResponse<void>> {
  return http.post(`/internships/${internshipId}/extend`, { additionalDays })
}

export function terminateInternship(internshipId: number): Promise<ApiResponse<void>> {
  return http.post(`/internships/${internshipId}/terminate`)
}
