import http from './axios'
import type { ApiResponse } from './axios'

export interface EmailTemplateDTO {
  id: number
  templateCode: string
  subjectTemplate: string
  bodyTemplate: string
  createdAt: string
  updatedAt: string
}

export interface EmailLogDTO {
  id: number
  recipient: string
  subject: string
  templateCode: string | null
  status: string
  failReason: string | null
  retryCount: number
  sentAt: string | null
  createdAt: string
}

export interface EmailConfigDTO {
  id: number
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  smtpPasswordEncrypted: string
  senderName: string
  sslEnabled: boolean
  updatedAt: string
}

export interface UpdateEmailConfigRequest {
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  smtpPassword: string | null
  senderName: string
  sslEnabled: boolean
}

export interface SendEmailRequest {
  recipient: string
  subject: string
  body: string
  html: boolean
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export function sendEmail(data: SendEmailRequest): Promise<ApiResponse<void>> {
  return http.post('/emails/send', data)
}

export function getTemplates(): Promise<ApiResponse<EmailTemplateDTO[]>> {
  return http.get('/emails/templates')
}

export function getLogs(page = 0, size = 20): Promise<ApiResponse<PageResult<EmailLogDTO>>> {
  return http.get('/emails/logs', { params: { page, size } })
}

export function getEmailConfig(): Promise<ApiResponse<EmailConfigDTO>> {
  return http.get('/emails/config')
}

export function updateEmailConfig(data: UpdateEmailConfigRequest): Promise<ApiResponse<void>> {
  return http.put('/emails/config', data)
}
