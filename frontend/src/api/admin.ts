import http from './axios'
import type { ApiResponse } from './axios'

export interface UserDTO {
  id: number
  username: string
  role: string
  enabled: boolean
  pendingDismissal: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  role: string
  enabled: string
}

export interface UpdateUserRequest {
  role?: string
  enabled?: string
  password?: string
}

export function listAllUsers(): Promise<ApiResponse<UserDTO[]>> {
  return http.get('/admin/users')
}

export function getUserById(id: number): Promise<ApiResponse<UserDTO>> {
  return http.get(`/admin/users/${id}`)
}

export function createUser(data: CreateUserRequest): Promise<ApiResponse<UserDTO>> {
  return http.post('/admin/users', data)
}

export function updateUser(id: number, data: UpdateUserRequest): Promise<ApiResponse<UserDTO>> {
  return http.put(`/admin/users/${id}`, data)
}

export function deleteUser(id: number): Promise<ApiResponse<void>> {
  return http.delete(`/admin/users/${id}`)
}
