import http from './axios'
import type { ApiResponse } from './axios'

export interface InterviewDTO {
  id: number
  applicationId: number
  userId: number
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PENDING_REVIEW' | 'REVIEWED'
  scenarioId: string | null
  difficultyLevel: string | null
  createdAt: string
  completedAt: string | null
}

export interface InterviewMessageDTO {
  id: number
  interviewId: number
  role: string
  content: string
  timestamp: string
  timeLimitSeconds: number
}

export interface InterviewReportDTO {
  id: number
  interviewId: number
  ruleFamiliarity: number
  communicationScore: number
  pressureScore: number
  totalScore: number
  aiComment: string | null
  reviewerComment: string | null
  reviewResult: string | null
  suggestedMentor: string | null
  recommendationLabel: string | null
  manualApproved: boolean | null
  reviewedAt: string | null
  createdAt: string
}

export function startInterview(applicationId: number, scenarioId: string): Promise<ApiResponse<InterviewDTO>> {
  return http.post('/interviews/start', { applicationId, scenarioId })
}

export function sendMessage(interviewId: number, message: string): Promise<ApiResponse<InterviewMessageDTO>> {
  return http.post(`/interviews/${interviewId}/message`, { message })
}

export function endInterview(interviewId: number): Promise<ApiResponse<InterviewReportDTO>> {
  return http.post(`/interviews/${interviewId}/end`)
}

export function getInterview(interviewId: number): Promise<ApiResponse<InterviewDTO>> {
  return http.get(`/interviews/${interviewId}`)
}

export function getMessages(interviewId: number): Promise<ApiResponse<InterviewMessageDTO[]>> {
  return http.get(`/interviews/${interviewId}/messages`)
}

export function getReport(interviewId: number): Promise<ApiResponse<InterviewReportDTO>> {
  return http.get(`/interviews/${interviewId}/report`)
}

export function submitReview(
  interviewId: number,
  data: { approved: boolean; reviewComment?: string; suggestedMentor?: string },
): Promise<ApiResponse<void>> {
  return http.post(`/interviews/${interviewId}/review`, data)
}
